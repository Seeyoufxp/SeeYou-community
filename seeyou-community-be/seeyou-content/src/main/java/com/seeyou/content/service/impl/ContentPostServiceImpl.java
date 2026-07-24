package com.seeyou.content.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seeyou.common.context.UserContext;
import com.seeyou.common.exception.BusinessException;
import com.seeyou.common.result.PageResult;
import com.seeyou.common.result.ResultCode;
import com.seeyou.common.utils.RedisUtils;
import com.seeyou.content.client.UserBriefLoader;
import com.seeyou.content.mapper.IContentDetailMapper;
import com.seeyou.content.mapper.IContentPostMapper;
import com.seeyou.content.pojo.dto.PostPublishDTO;
import com.seeyou.content.pojo.dto.PostQueryDTO;
import com.seeyou.content.pojo.dto.PostUpdateDTO;
import com.seeyou.content.pojo.dto.UserBriefDTO;
import com.seeyou.content.pojo.entity.ContentDetail;
import com.seeyou.content.pojo.entity.ContentPost;
import com.seeyou.content.pojo.enums.ContentType;
import com.seeyou.content.pojo.event.ContentEventMsg;
import com.seeyou.content.pojo.vo.LikeResultVO;
import com.seeyou.content.pojo.vo.PostDetailVO;
import com.seeyou.content.pojo.vo.PostListVO;
import com.seeyou.content.pojo.vo.PostSearchDocVO;
import com.seeyou.content.service.IContentPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 内容服务实现
 * 帖子/博客/问答共用 content_post 表，type 区分。
 * 点赞：Redis Set 维护用户点赞关系 + Redis 锁防并发 + SQL 原子加减 like_count
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentPostServiceImpl implements IContentPostService {

    private final IContentPostMapper contentPostMapper;
    private final IContentDetailMapper contentDetailMapper;
    private final RedisUtils redisUtils;
    private final RocketMQTemplate rocketMQTemplate;
    private final UserBriefLoader userBriefLoader;

    /** Redis 点赞集合 key 前缀，存 userId 字符串集合 */
    private static final String LIKE_SET_PREFIX = "like:post:";
    /** 点赞操作分布式锁前缀 */
    private static final String LIKE_LOCK_PREFIX = "lock:like:post:";
    private static final long LIKE_LOCK_TIMEOUT_MS = 5000L;
    /** 内容事件 Topic：content 服务生产 → search 服务消费同步 ES */
    private static final String CONTENT_EVENT_TOPIC = "content-event-topic";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publish(ContentType type, PostPublishDTO dto) {
        Long userId = requireLogin();

        ContentPost post = new ContentPost();
        post.setUserId(userId);
        post.setType(type.getCode());
        post.setTitle(dto.getTitle());
        post.setSummary(StrUtil.isBlank(dto.getSummary()) ? buildSummary(dto.getContent()) : dto.getSummary());
        post.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setCreateTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());
        post.setIsDeleted(0);
        contentPostMapper.insert(post);

        ContentDetail detail = new ContentDetail();
        detail.setPostId(post.getId());
        detail.setContent(dto.getContent());
        contentDetailMapper.insert(detail);

        log.info("发布内容: id={}, type={}, userId={}", post.getId(), type.getCode(), userId);
        sendEvent(post.getId(), ContentEventMsg.CREATE);
        return post.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PostUpdateDTO dto) {
        Long userId = requireLogin();
        ContentPost post = mustExist(id);
        if (!isOwnerOrAdmin(post.getUserId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权编辑他人内容");
        }

        if (StrUtil.isNotBlank(dto.getTitle())) {
            post.setTitle(dto.getTitle());
        }
        if (dto.getSummary() != null) {
            post.setSummary(dto.getSummary());
        }
        if (dto.getStatus() != null) {
            post.setStatus(dto.getStatus());
        }
        post.setUpdateTime(LocalDateTime.now());
        contentPostMapper.updateById(post);

        if (dto.getContent() != null) {
            ContentDetail detail = contentDetailMapper.selectById(id);
            if (detail != null) {
                detail.setContent(dto.getContent());
                contentDetailMapper.updateById(detail);
            } else {
                ContentDetail d = new ContentDetail();
                d.setPostId(id);
                d.setContent(dto.getContent());
                contentDetailMapper.insert(d);
            }
        }
        sendEvent(id, ContentEventMsg.UPDATE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long userId = requireLogin();
        ContentPost post = mustExist(id);
        if (!isOwnerOrAdmin(post.getUserId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权删除他人内容");
        }
        // content_post 逻辑删；评论表无 is_deleted 字段，按 postId 查询时 post 已逻辑删，前端不再访问
        contentPostMapper.deleteById(id);
        // 清理 Redis 点赞集合
        redisUtils.delete(LIKE_SET_PREFIX + id);
        log.info("删除内容: id={}, userId={}", id, userId);
        sendEvent(id, ContentEventMsg.DELETE);
    }

    @Override
    public PostDetailVO getDetail(Long id) {
        ContentPost post = mustExist(id);
        // 草稿/隐藏/封禁 仅作者本人或管理员可见
        if (post.getStatus() != 1 && !isOwnerOrAdmin(post.getUserId(), currentUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "内容不存在");
        }

        PostDetailVO vo = new PostDetailVO();
        BeanUtil.copyProperties(post, vo);
        ContentDetail detail = contentDetailMapper.selectById(id);
        vo.setContent(detail == null ? "" : detail.getContent());
        vo.setLiked(isLikedByCurrent(id));
        // 填充作者信息（复用批量加载器，传单元素集合）
        Map<Long, UserBriefDTO> userMap = userBriefLoader.loadByIds(Set.of(post.getUserId()));
        UserBriefDTO u = userMap.get(post.getUserId());
        if (u != null) {
            vo.setNickname(u.getNickname());
            vo.setAvatarUrl(u.getAvatarUrl());
        }
        return vo;
    }

    @Override
    public PageResult<PostListVO> list(ContentType type, PostQueryDTO query) {
        int current = query.getCurrent() == null || query.getCurrent() < 1 ? 1 : query.getCurrent();
        int size = query.getSize() == null || query.getSize() < 1 ? 10 : Math.min(query.getSize(), 50);

        LambdaQueryWrapper<ContentPost> wrapper = Wrappers.<ContentPost>lambdaQuery()
                .eq(ContentPost::getType, type.getCode())
                .eq(ContentPost::getStatus, query.getStatus() == null ? 1 : query.getStatus());

        if (StrUtil.isNotBlank(query.getKeyword())) {
            String kw = query.getKeyword();
            wrapper.and(w -> w.like(ContentPost::getTitle, kw).or().like(ContentPost::getSummary, kw));
        }
        if (query.getUserId() != null) {
            wrapper.eq(ContentPost::getUserId, query.getUserId());
        }

        // 排序字段白名单，防注入
        String sortBy = safeSortBy(query.getSortBy());
        boolean asc = "asc".equalsIgnoreCase(query.getOrder());
        switch (sortBy) {
            case "like_count" -> wrapper.orderBy(true, asc, ContentPost::getLikeCount);
            case "comment_count" -> wrapper.orderBy(true, asc, ContentPost::getCommentCount);
            default -> wrapper.orderBy(true, asc, ContentPost::getCreateTime);
        }

        IPage<ContentPost> page = contentPostMapper.selectPage(new Page<>(current, size), wrapper);

        // 批量拉取作者信息，避免逐条 Feign 调用导致 N+1
        List<ContentPost> posts = page.getRecords();
        Set<Long> userIds = posts.stream().map(ContentPost::getUserId).collect(Collectors.toSet());
        Map<Long, UserBriefDTO> userMap = userBriefLoader.loadByIds(userIds);

        List<PostListVO> records = posts.stream().map(p -> {
            PostListVO vo = new PostListVO();
            BeanUtil.copyProperties(p, vo);
            vo.setLiked(isLikedByCurrent(p.getId()));
            UserBriefDTO u = userMap.get(p.getUserId());
            if (u != null) {
                vo.setNickname(u.getNickname());
                vo.setAvatarUrl(u.getAvatarUrl());
            }
            return vo;
        }).collect(Collectors.toList());
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public LikeResultVO like(Long id) {
        Long userId = requireLogin();
        mustExist(id);
        String setKey = LIKE_SET_PREFIX + id;
        String lockKey = LIKE_LOCK_PREFIX + id + ":" + userId;

        if (!Boolean.TRUE.equals(redisUtils.setIfAbsent(lockKey, "1", LIKE_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS))) {
            throw new BusinessException("操作太频繁，请稍后再试");
        }
        try {
            Long added = redisUtils.sAdd(setKey, String.valueOf(userId));
            if (added != null && added > 0) {
                contentPostMapper.incrLikeCount(id);
            }
            Integer count = contentPostMapper.selectById(id).getLikeCount();
            return new LikeResultVO(Boolean.TRUE, count);
        } finally {
            redisUtils.delete(lockKey);
        }
    }

    @Override
    public LikeResultVO unlike(Long id) {
        Long userId = requireLogin();
        mustExist(id);
        String setKey = LIKE_SET_PREFIX + id;
        String lockKey = LIKE_LOCK_PREFIX + id + ":" + userId;

        if (!Boolean.TRUE.equals(redisUtils.setIfAbsent(lockKey, "1", LIKE_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS))) {
            throw new BusinessException("操作太频繁，请稍后再试");
        }
        try {
            Long removed = redisUtils.sRemove(setKey, String.valueOf(userId));
            if (removed != null && removed > 0) {
                contentPostMapper.decrLikeCount(id);
            }
            Integer count = contentPostMapper.selectById(id).getLikeCount();
            return new LikeResultVO(Boolean.FALSE, count);
        } finally {
            redisUtils.delete(lockKey);
        }
    }

    @Override
    public PostSearchDocVO getSearchDoc(Long id) {
        ContentPost post = contentPostMapper.selectById(id);
        // 逻辑删后 selectById 直接返回 null；status != 1 视为不可搜索
        if (post == null || post.getStatus() == null || post.getStatus() != 1) {
            return null;
        }
        PostSearchDocVO vo = new PostSearchDocVO();
        vo.setId(post.getId());
        vo.setType(post.getType());
        vo.setTitle(post.getTitle());
        vo.setSummary(post.getSummary());
        ContentDetail detail = contentDetailMapper.selectById(id);
        // ES text 字段无强制长度限制，但截断保护：避免极端长文撑爆单条文档
        vo.setContent(stripAndTruncate(detail == null ? "" : detail.getContent(), 8000));
        vo.setUserId(post.getUserId());
        vo.setLikeCount(post.getLikeCount());
        vo.setCommentCount(post.getCommentCount());
        vo.setStatus(post.getStatus());
        vo.setCreateTime(post.getCreateTime());
        vo.setUpdateTime(post.getUpdateTime());
        // 复用批量加载器，传单元素集合，避免新增专门的 getById Feign 调用
        Map<Long, UserBriefDTO> userMap = userBriefLoader.loadByIds(Set.of(post.getUserId()));
        UserBriefDTO u = userMap.get(post.getUserId());
        if (u != null) {
            vo.setNickname(u.getNickname());
            vo.setAvatarUrl(u.getAvatarUrl());
        }
        return vo;
    }

    // tools

    private Long requireLogin() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        return userId;
    }

    private Long currentUserId() {
        return UserContext.getUserId();
    }

    private ContentPost mustExist(Long id) {
        ContentPost post = contentPostMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "内容不存在");
        }
        return post;
    }

    private boolean isOwnerOrAdmin(Long ownerId, Long currentUserId) {
        if (currentUserId == null) return false;
        if (ownerId.equals(currentUserId)) return true;
        Integer role = UserContext.getRole();
        return role != null && role >= 1;
    }

    private boolean isLikedByCurrent(Long postId) {
        Long userId = currentUserId();
        if (userId == null) return false;
        return Boolean.TRUE.equals(redisUtils.sIsMember(LIKE_SET_PREFIX + postId, String.valueOf(userId)));
    }

    private String safeSortBy(String sortBy) {
        if (sortBy == null) return "create_time";
        Set<String> allowed = Set.of("create_time", "like_count", "comment_count");
        return allowed.contains(sortBy) ? sortBy : "create_time";
    }

    private String buildSummary(String content) {
        if (StrUtil.isBlank(content)) return "";
        String plain = content.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
        return plain.length() > 200 ? plain.substring(0, 200) : plain;
    }

    /** 剥离 HTML 标签 + 折叠空白 + 截断到 maxLen 字符，用于 ES 索引的纯文本正文 */
    private String stripAndTruncate(String content, int maxLen) {
        if (StrUtil.isBlank(content)) return "";
        String plain = content.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
        return plain.length() > maxLen ? plain.substring(0, maxLen) : plain;
    }

    /**
     * 发送内容事件到 search 服务（同步 ES）
     * 在事务提交后发送，避免事务回滚但消息已发，search 回查时拿不到数据。
     * MQ 发送失败只记日志、不抛异常——事务已提交，DB 已写入，
     * 不能因为 MQ 故障让发布接口返回失败（否则用户重试会导致重复发布）。
     * 后续可补一张本地消息表做重试，保证最终一致。
     */
    private void sendEvent(Long id, String action) {
        Runnable send = () -> {
            try {
                rocketMQTemplate.syncSend(CONTENT_EVENT_TOPIC,
                        MessageBuilder.withPayload(ContentEventMsg.of(id, action)).build());
                log.info("发送内容事件: id={}, action={}", id, action);
            } catch (Exception e) {
                log.error("发送内容事件失败(不影响发布结果): id={}, action={}", id, action, e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }
}
