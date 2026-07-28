package com.seeyou.chat.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seeyou.chat.pojo.dto.ChatMessageMsg;
import com.seeyou.chat.websocket.ChatWebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * 聊天消息 MQ 消费者（广播模式）。
 * <p>
 * application.yml 配置 spring.cloud.stream.rocketmq.bindings.chatMessage-in-0.consumer.messageModel=BROADCASTING，
 * 每个 chat 实例都消费全量消息。收到后通过 SessionManager 推给本实例连接的 WebSocket 客户端。
 * <p>
 * 发送方实例也会收到自己发的消息——这是正常的：客户端发消息走 HTTP，
 * WebSocket 回推正好作为消息确认 + 统一渲染入口。
 * <p>
 * 消费失败 try-catch 不抛，避免广播模式下无限重投。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageConsumer {

    private final ChatWebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    /**
     * 函数式 Consumer Bean，名字 chatMessage 对应 binding chatMessage-in-0。
     * 需在 application.yml 配 spring.cloud.function.definition=chatMessage。
     */
    @Bean
    public Consumer<ChatMessageMsg> chatMessage() {
        return msg -> {
            if (msg == null || msg.getRoomId() == null) {
                log.warn("收到空聊天消息，忽略");
                return;
            }
            try {
                String json = objectMapper.writeValueAsString(msg);
                sessionManager.send(msg.getRoomId(), json);
                log.debug("聊天消息已推送本实例连接: roomId={}, messageId={}", msg.getRoomId(), msg.getId());
            } catch (Exception e) {
                log.error("处理聊天消息失败(不影响其他实例): roomId={}, messageId={}",
                        msg.getRoomId(), msg.getId(), e);
            }
        };
    }
}
