package com.seeyou.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seeyou.chat.client.UserBriefLoader;
import com.seeyou.chat.constant.ChatConstants;
import com.seeyou.chat.mapper.IChatMessageMapper;
import com.seeyou.chat.pojo.dto.ChatMessageMsg;
import com.seeyou.chat.pojo.dto.ChatMessageQueryDTO;
import com.seeyou.chat.pojo.dto.ChatMessageSendDTO;
import com.seeyou.chat.pojo.dto.UserBriefDTO;
import com.seeyou.chat.pojo.entity.ChatMessage;
import com.seeyou.chat.pojo.vo.ChatMessageVO;
import com.seeyou.chat.service.IChatMessageService;
import com.seeyou.chat.service.IChatRoomService;
import com.seeyou.common.context.UserContext;
import com.seeyou.common.exception.BusinessException;
import com.seeyou.common.result.PageResult;
import com.seeyou.common.result.ResultCode;
import com.seeyou.common.utils.IdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 聊天消息服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements IChatMessageService {

    private final IChatMessageMapper chatMessageMapper;
    private final IChatRoomService chatRoomService;
    private final UserBriefLoader userBriefLoader;
    private final StreamBridge streamBridge;

    @Override
    public ChatMessageVO send(ChatMessageSendDTO dto) {
        Long userId = requireLogin();
        // 校验聊天室存在且正常
        chatRoomService.checkRoomAvailable(dto.getRoomId());

        String content = dto.getContent().trim();
        if (content.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "消息内容不能为空");
        }

        // 落库（雪花算法 ID）
        LocalDateTime now = LocalDateTime.now();
        ChatMessage msg = new ChatMessage();
        msg.setId(IdWorker.nextId());
        msg.setRoomId(dto.getRoomId());
        msg.setUserId(userId);
        msg.setContent(content);
        msg.setCreateTime(now);
        chatMessageMapper.insert(msg);

        // 拉取发送者信息（推送用）
        UserBriefDTO sender = userBriefLoader.loadById(userId);
        String nickname = sender == null ? "" : sender.getNickname();
        String avatarUrl = sender == null ? "" : sender.getAvatarUrl();

        // 组装 MQ 消息体（= WebSocket 推送内容），广播给所有 chat 实例
        ChatMessageMsg mqMsg = ChatMessageMsg.of(msg.getId(), msg.getRoomId(), userId,
                nickname, avatarUrl, content, now);
        try {
            streamBridge.send(ChatConstants.CHAT_MESSAGE_OUT_BINDING,
                    MessageBuilder.withPayload(mqMsg).build());
        } catch (Exception e) {
            // MQ 发送失败不影响落库结果（消息已持久化，历史可查），
            // 仅影响其他客户端实时推送；发送者客户端凭 HTTP 响应正常显示。
            log.error("聊天消息 MQ 广播失败(不影响落库): roomId={}, messageId={}",
                    msg.getRoomId(), msg.getId(), e);
        }

        // 返回完整 VO，客户端立即渲染
        return toVO(msg, nickname, avatarUrl);
    }

    @Override
    public PageResult<ChatMessageVO> history(ChatMessageQueryDTO query) {
        requireLogin();
        Long roomId = query.getRoomId();
        // 校验聊天室存在且状态正常（已解散房间不允许查历史）
        chatRoomService.checkRoomAvailable(roomId);

        long c = query.getCurrent() == null || query.getCurrent() < 1 ? 1 : query.getCurrent();
        long s = query.getSize() == null || query.getSize() < 1
                ? ChatConstants.HISTORY_DEFAULT_SIZE
                : Math.min(query.getSize(), ChatConstants.HISTORY_MAX_SIZE);

        Page<ChatMessage> page = new Page<>(c, s);
        // 倒序：最新消息在前；同毫秒内按 id 倒序保证顺序稳定
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getRoomId, roomId)
                .orderByDesc(ChatMessage::getCreateTime)
                .orderByDesc(ChatMessage::getId);
        Page<ChatMessage> result = chatMessageMapper.selectPage(page, wrapper);

        List<ChatMessage> records = result.getRecords();
        if (records.isEmpty()) {
            return PageResult.empty(c, s);
        }

        // 批量拉发送者信息
        Set<Long> userIds = records.stream().map(ChatMessage::getUserId).collect(Collectors.toSet());
        Map<Long, UserBriefDTO> userMap = userBriefLoader.loadByIds(userIds);

        List<ChatMessageVO> voList = records.stream()
                .map(m -> toVO(m, userMap.get(m.getUserId())))
                .collect(Collectors.toList());
        return PageResult.of(voList, result.getTotal(), c, s);
    }

    // ===== 内部工具 =====

    private Long requireLogin() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        return userId;
    }

    private ChatMessageVO toVO(ChatMessage msg, UserBriefDTO sender) {
        return toVO(msg, sender == null ? "" : sender.getNickname(),
                sender == null ? "" : sender.getAvatarUrl());
    }

    private ChatMessageVO toVO(ChatMessage msg, String nickname, String avatarUrl) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(msg.getId());
        vo.setRoomId(msg.getRoomId());
        vo.setUserId(msg.getUserId());
        vo.setNickname(nickname);
        vo.setAvatarUrl(avatarUrl);
        vo.setContent(msg.getContent());
        vo.setCreateTime(msg.getCreateTime());
        return vo;
    }
}
