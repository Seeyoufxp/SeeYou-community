package com.seeyou.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seeyou.chat.constant.ChatConstants;
import com.seeyou.chat.mapper.IChatRoomMapper;
import com.seeyou.chat.pojo.dto.ChatRoomCreateDTO;
import com.seeyou.chat.pojo.dto.ChatRoomUpdateDTO;
import com.seeyou.chat.pojo.dto.UserBriefDTO;
import com.seeyou.chat.pojo.entity.ChatRoom;
import com.seeyou.chat.pojo.vo.ChatRoomVO;
import com.seeyou.chat.pojo.vo.OnlineMemberVO;
import com.seeyou.chat.client.UserBriefLoader;
import com.seeyou.chat.service.IChatRoomService;
import com.seeyou.common.context.UserContext;
import com.seeyou.common.exception.BusinessException;
import com.seeyou.common.result.PageResult;
import com.seeyou.common.result.ResultCode;
import com.seeyou.common.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 聊天室服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements IChatRoomService {

    private final IChatRoomMapper chatRoomMapper;
    private final UserBriefLoader userBriefLoader;
    private final RedisUtils redisUtils;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Long create(ChatRoomCreateDTO dto) {
        Long userId = requireLogin();
        if (!isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "仅管理员可创建聊天室");
        }
        ChatRoom room = new ChatRoom();
        room.setName(dto.getName().trim());
        room.setDescription(dto.getDescription() == null ? "" : dto.getDescription().trim());
        room.setCreatorId(userId);
        room.setStatus(ChatConstants.ROOM_STATUS_NORMAL);
        room.setCreateTime(LocalDateTime.now());
        chatRoomMapper.insert(room);
        log.info("创建聊天室: id={}, name={}, creator={}", room.getId(), room.getName(), userId);
        return room.getId();
    }

    @Override
    public void update(Long id, ChatRoomUpdateDTO dto) {
        Long userId = requireLogin();
        ChatRoom room = mustExist(id);
        checkOwnerOrAdmin(room, userId);

        boolean changed = false;
        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            room.setName(dto.getName().trim());
            changed = true;
        }
        if (dto.getDescription() != null) {
            room.setDescription(dto.getDescription().trim());
            changed = true;
        }
        if (changed) {
            chatRoomMapper.updateById(room);
            log.info("修改聊天室: id={}, operator={}", id, userId);
        }
    }

    @Override
    public void dissolve(Long id) {
        Long userId = requireLogin();
        ChatRoom room = mustExist(id);
        checkOwnerOrAdmin(room, userId);

        room.setStatus(ChatConstants.ROOM_STATUS_DISSOLVED);
        chatRoomMapper.updateById(room);
        // 清理在线集合
        redisUtils.delete(ChatConstants.REDIS_ONLINE_KEY_PREFIX + id);
        log.info("解散聊天室: id={}, operator={}", id, userId);
    }

    @Override
    public PageResult<ChatRoomVO> list(Long current, Long size) {
        long c = current == null || current < 1 ? 1 : current;
        long s = size == null || size < 1 ? 10 : Math.min(size, 50);

        Page<ChatRoom> page = new Page<>(c, s);
        LambdaQueryWrapper<ChatRoom> wrapper = new LambdaQueryWrapper<ChatRoom>()
                .eq(ChatRoom::getStatus, ChatConstants.ROOM_STATUS_NORMAL)
                .orderByDesc(ChatRoom::getCreateTime);
        Page<ChatRoom> result = chatRoomMapper.selectPage(page, wrapper);

        List<ChatRoom> rooms = result.getRecords();
        if (rooms.isEmpty()) {
            return PageResult.empty(c, s);
        }

        // 批量拉创建者信息
        Set<Long> creatorIds = rooms.stream().map(ChatRoom::getCreatorId).collect(Collectors.toSet());
        Map<Long, UserBriefDTO> userMap = userBriefLoader.loadByIds(creatorIds);

        List<ChatRoomVO> voList = rooms.stream().map(room -> toVO(room, userMap)).collect(Collectors.toList());
        return PageResult.of(voList, result.getTotal(), c, s);
    }

    @Override
    public ChatRoomVO getDetail(Long id) {
        ChatRoom room = mustExist(id);
        Map<Long, UserBriefDTO> userMap = userBriefLoader.loadByIds(Collections.singleton(room.getCreatorId()));
        return toVO(room, userMap);
    }

    @Override
    public Long getOnlineCount(Long roomId) {
        if (roomId == null) {
            return 0L;
        }
        Long size = redisUtils.sSize(ChatConstants.REDIS_ONLINE_KEY_PREFIX + roomId);
        return size == null ? 0L : size;
    }

    @Override
    public List<OnlineMemberVO> getOnlineMembers(Long roomId) {
        if (roomId == null) {
            return Collections.emptyList();
        }
        Set<String> userIdStrs = stringRedisTemplate.opsForSet()
                .members(ChatConstants.REDIS_ONLINE_KEY_PREFIX + roomId);
        if (userIdStrs == null || userIdStrs.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = new ArrayList<>();
        for (String s : userIdStrs) {
            try {
                userIds.add(Long.parseLong(s));
            } catch (NumberFormatException ignored) {
            }
        }
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, UserBriefDTO> userMap = userBriefLoader.loadByIds(userIds);
        List<OnlineMemberVO> list = new ArrayList<>(userIds.size());
        for (Long uid : userIds) {
            UserBriefDTO u = userMap.get(uid);
            OnlineMemberVO vo = new OnlineMemberVO();
            vo.setUserId(uid);
            if (u != null) {
                vo.setNickname(u.getNickname());
                vo.setAvatarUrl(u.getAvatarUrl());
            }
            list.add(vo);
        }
        return list;
    }

    @Override
    public ChatRoom checkRoomAvailable(Long roomId) {
        ChatRoom room = chatRoomMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "聊天室不存在");
        }
        if (room.getStatus() == null || room.getStatus() != ChatConstants.ROOM_STATUS_NORMAL) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "聊天室已解散或封禁");
        }
        return room;
    }

    // ===== 内部工具 =====

    private Long requireLogin() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        return userId;
    }

    private boolean isAdmin() {
        Integer role = UserContext.getRole();
        return role != null && role >= 1;
    }

    private ChatRoom mustExist(Long id) {
        ChatRoom room = chatRoomMapper.selectById(id);
        if (room == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "聊天室不存在");
        }
        return room;
    }

    private void checkOwnerOrAdmin(ChatRoom room, Long userId) {
        boolean owner = room.getCreatorId() != null && room.getCreatorId().equals(userId);
        if (!owner && !isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权限操作该聊天室");
        }
    }

    private ChatRoomVO toVO(ChatRoom room, Map<Long, UserBriefDTO> userMap) {
        ChatRoomVO vo = new ChatRoomVO();
        vo.setId(room.getId());
        vo.setName(room.getName());
        vo.setDescription(room.getDescription());
        vo.setCreatorId(room.getCreatorId());
        vo.setCreator(userMap.get(room.getCreatorId()));
        vo.setStatus(room.getStatus());
        vo.setCreateTime(room.getCreateTime());
        vo.setOnlineCount(getOnlineCount(room.getId()));
        return vo;
    }
}
