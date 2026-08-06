package com.seeyou.chat.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天消息 MQ 广播消息体（也是 WebSocket 推送给客户端的完整内容）
 * <p>
 * 发送方落库后拉取用户昵称/头像组装此对象，通过 StreamBridge 发到 chat-message-topic；
 * 各 chat 实例（广播模式）消费后直接转 JSON 推给本实例连接的 WebSocket 客户端。
 * 消费方零额外 Feign 调用，避免每个实例每条消息重复拉用户信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageMsg implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 消息ID（雪花算法） */
    private Long id;
    /** 聊天室ID */
    private Long roomId;
    /** 发送者ID */
    private Long userId;
    /** 发送者昵称（推送时展示） */
    private String nickname;
    /** 发送者头像URL */
    private String avatarUrl;
    /** 消息内容 */
    private String content;
    /** 发送时间 */
    private LocalDateTime createTime;

    public static ChatMessageMsg of(Long id, Long roomId, Long userId, String nickname,
                                    String avatarUrl, String content, LocalDateTime createTime) {
        return new ChatMessageMsg(id, roomId, userId, nickname, avatarUrl, content, createTime);
    }
}
