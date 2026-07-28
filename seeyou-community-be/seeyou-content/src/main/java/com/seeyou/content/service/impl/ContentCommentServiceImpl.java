package com.seeyou.content.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.seeyou.common.context.UserContext;
import com.seeyou.common.exception.BusinessException;
import com.seeyou.common.result.ResultCode;
import com.seeyou.common.utils.RedisUtils;
import com.seeyou.content.client.UserBriefLoader;
import com.seeyou.content.mapper.IContentCommentMapper;
import com.seeyou.content.mapper.IContentPostMapper;
import com.seeyou.content.pojo.dto.CommentCreateDTO;
import com.seeyou.content.pojo.dto.UserBriefDTO;
import com.seeyou.content.pojo.entity.ContentComment;
import com.seeyou.content.pojo.entity.ContentPost;
import com.seeyou.content.pojo.enums.ContentType;
import com.seeyou.content.pojo.event.ContentEventMsg;
import com.seeyou.content.pojo.vo.CommentVO;
import com.seeyou.content.pojo.vo.LikeResultVO;
import com.seeyou.content.service.IContentCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 评论/回答服务实现
 *
 * 楼中楼规则（核心）：
 *   - 问答(type=3)的回答：parent_id 必须 = 0，不支持楼中楼
 *   - 帖子/博客(type=1/2)的评论：parent_id=0 为一级评论；
 *     parent_id 指向某条一级评论时为楼中楼（只支持一层，禁止在楼中楼上再回复）
 *
 * 知识库同步：
 *   - 问答的回答是知识库内容的一部分（拼到 post content 后面做 embedding）
 *   - 新增/删除问答的回答后，发 UPDATE 事件让 search/ai 重新拉问答数据同步 ES/向量库
 *   - 普通帖子/博客的评论不进知识库，不发事件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentCommentServiceImpl implements IContentCommentService {

    private final IContentCommentMapper contentCommentMapper;
    private final IContentPostMapper contentPostMapper;
    private final RedisUtils redisUtils;
    private final UserBriefLoader userBriefLoader;
    private final RocketMQTemplate rocketMQTemplate;

    private static final String LIKE_SET_PREFIX = "like:comment:";
    private static final String LIKE_LOCK_PREFIX = "lock:like:comment:";
    private static final long LIKE_LOCK_TIMEOUT_MS = 5000L;
    /** 内容事件 Topic：与 ContentPostServiceImpl 共用 */
    private static final String CONTENT_EVENT_TOPIC = "content-event-topic";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createComment(CommentCreateDTO dto) {
        Long userId = requireLogin();

        ContentPost post = contentPostMapper.selectById(dto.getPostId());
        if (post == null || post.getStatus() == null || post.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "内容不存在或未发布");
        }

        Long parentId = dto.getParentId() == null ? 0L : dto.getParentId();
        validateParentRule(post, parentId);

        ContentComment comment = new ContentComment();
        comment.setPostId(dto.getPostId());
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setContent(dto.getContent());
        comment.setLikeCount(0);
        comment.setCreateTime(LocalDateTime.now());
        contentCommentMapper.insert(comment);

        contentCommentMapper.incrPostCommentCount(dto.getPostId());
        log.info("创建评论: id={}, postId={}, parentId={}, userId={}",
                comment.getId(), dto.getPostId(), parentId, userId);

        // 问答的回答会进知识库（拼到 post content），新增回答后通知 search/ai 重新同步
        if (post.getType() != null && post.getType() == ContentType.QA.getCode()) {
            publishContentEventAfterCommit(post.getId(), ContentEventMsg.UPDATE);
        }
        return comment.getId();
    }

    /**
     * 事务提交后发 MQ 事件，通知 search/ai 服务重新拉数据。
     * 失败降级不抛异常，避免 MQ 故障让发布接口 500（事务已提交会导致重复发布）。
     */
    private void publishContentEventAfterCommit(Long postId, String action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 无事务上下文（理论不会），直接发
            try {
                rocketMQTemplate.syncSend(CONTENT_EVENT_TOPIC,
                        MessageBuilder.withPayload(ContentEventMsg.of(postId, action)).build());
            } catch (Exception e) {
                log.warn("MQ 同步发送失败(无事务): postId={}, action={}", postId, action, e);
            }
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    rocketMQTemplate.syncSend(CONTENT_EVENT_TOPIC,
                            MessageBuilder.withPayload(ContentEventMsg.of(postId, action)).build());
                } catch (Exception e) {
                    log.warn("MQ 同步发送失败(afterCommit): postId={}, action={}", postId, action, e);
                }
            }
        });
    }

    /**
     * 楼中楼规则校验：
     *   - 问答(type=3)：parentId 必须 0
     *   - 帖子/博客(type=1/2)：parentId 非 0 时，必须指向同一 post 的一级评论（其 parent_id==0）
     */
    private void validateParentRule(ContentPost post, Long parentId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        if (post.getType() == ContentType.QA.getCode()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "问答的回答不支持楼中楼");
        }
        ContentComment parent = contentCommentMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "父评论不存在");
        }
        if (!parent.getPostId().equals(post.getId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "父评论与所属内容不匹配");
        }
        if (parent.getParentId() != null && parent.getParentId() != 0L) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "楼中楼只支持一层，不能在回复上再回复");
        }
    }

    @Override
    public List<CommentVO> listComments(Long postId) {
        if (postId == null) {
            return Collections.emptyList();
        }
        // post 不存在或被逻辑删时返回空列表，不抛异常，避免详情页报错
        ContentPost post = contentPostMapper.selectById(postId);
        if (post == null) {
            return Collections.emptyList();
        }

        List<ContentComment> all = contentCommentMapper.selectList(
                Wrappers.<ContentComment>lambdaQuery()
                        .eq(ContentComment::getPostId, postId)
                        .orderByAsc(ContentComment::getCreateTime));
        if (all.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量拉取所有评论者（含楼中楼）的概要信息
        Set<Long> userIds = all.stream().map(ContentComment::getUserId).collect(Collectors.toSet());
        Map<Long, UserBriefDTO> userMap = userBriefLoader.loadByIds(userIds);

        Map<Long, List<ContentComment>> childrenMap = new HashMap<>();
        List<ContentComment> roots = new ArrayList<>();
        for (ContentComment c : all) {
            if (c.getParentId() == null || c.getParentId() == 0L) {
                roots.add(c);
            } else {
                childrenMap.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c);
            }
        }

        List<CommentVO> result = new ArrayList<>(roots.size());
        for (ContentComment root : roots) {
            CommentVO vo = toVO(root, userMap);
            List<ContentComment> children = childrenMap.getOrDefault(root.getId(), Collections.emptyList());
            List<CommentVO> childVOs = children.stream().map(c -> toVO(c, userMap)).collect(Collectors.toList());
            vo.setChildren(childVOs);
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long userId = requireLogin();
        ContentComment comment = contentCommentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "评论不存在");
        }
        if (!isOwnerOrAdmin(comment.getUserId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权删除他人评论");
        }

        int delta = 1;
        // 一级评论：连带删除其下所有楼中楼
        if (comment.getParentId() == null || comment.getParentId() == 0L) {
            Long childCount = contentCommentMapper.selectCount(
                    Wrappers.<ContentComment>lambdaQuery().eq(ContentComment::getParentId, id));
            if (childCount != null && childCount > 0) {
                contentCommentMapper.delete(Wrappers.<ContentComment>lambdaQuery().eq(ContentComment::getParentId, id));
                delta += childCount;
            }
        }
        contentCommentMapper.deleteById(id);
        contentCommentMapper.decrPostCommentCountBy(comment.getPostId(), delta);
        // 清理 Redis 点赞集合
        redisUtils.delete(LIKE_SET_PREFIX + id);
        log.info("删除评论: id={}, userId={}, 实际删除数={}", id, userId, delta);

        // 如果删的是问答的回答，通知 search/ai 重新同步（回答已不在向量库需要更新）
        ContentPost post = contentPostMapper.selectById(comment.getPostId());
        if (post != null && post.getType() != null && post.getType() == ContentType.QA.getCode()
                && (comment.getParentId() == null || comment.getParentId() == 0L)) {
            publishContentEventAfterCommit(post.getId(), ContentEventMsg.UPDATE);
        }
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
                contentCommentMapper.incrLikeCount(id);
            }
            Integer count = contentCommentMapper.selectById(id).getLikeCount();
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
                contentCommentMapper.decrLikeCount(id);
            }
            Integer count = contentCommentMapper.selectById(id).getLikeCount();
            return new LikeResultVO(Boolean.FALSE, count);
        } finally {
            redisUtils.delete(lockKey);
        }
    }

    // tool

    private CommentVO toVO(ContentComment c, Map<Long, UserBriefDTO> userMap) {
        CommentVO vo = new CommentVO();
        BeanUtil.copyProperties(c, vo);
        vo.setLiked(isLikedByCurrent(c.getId()));
        UserBriefDTO u = userMap.get(c.getUserId());
        if (u != null) {
            vo.setNickname(u.getNickname());
            vo.setAvatarUrl(u.getAvatarUrl());
        }
        return vo;
    }

    private void mustExist(Long id) {
        if (contentCommentMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "评论不存在");
        }
    }

    private Long requireLogin() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        return userId;
    }

    private boolean isOwnerOrAdmin(Long ownerId, Long currentUserId) {
        if (currentUserId == null) return false;
        if (ownerId.equals(currentUserId)) return true;
        Integer role = UserContext.getRole();
        return role != null && role >= 1;
    }

    private boolean isLikedByCurrent(Long commentId) {
        Long userId = UserContext.getUserId();
        if (userId == null) return false;
        return Boolean.TRUE.equals(redisUtils.sIsMember(LIKE_SET_PREFIX + commentId, String.valueOf(userId)));
    }
}
