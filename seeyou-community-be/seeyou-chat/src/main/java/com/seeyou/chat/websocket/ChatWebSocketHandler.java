package com.seeyou.chat.websocket;

import com.seeyou.chat.constant.ChatConstants;
import com.seeyou.common.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

/**
 * 聊天 WebSocket 消息处理器。
 * <p>
 * 业务消息发送走 HTTP（/api/chat/message/send）落库 + MQ 广播，WebSocket 只负责：
 * 1. 连接建立/关闭时维护 Redis 在线用户集合（统计在线人数）
 * 2. 接收客户端心跳（ping/pong），保活连接
 * 3. 由 SessionManager 向本房间连接推送 MQ 广播过来的消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final RedisUtils redisUtils;
    private final ChatWebSocketSessionManager sessionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Map<String, Object> attrs = session.getAttributes();
        Long userId = (Long) attrs.get("userId");
        Long roomId = (Long) attrs.get("roomId");

        if (userId == null || roomId == null) {
            log.warn("WebSocket 连接缺少用户/房间信息，关闭: sessionId={}", session.getId());
            closeQuietly(session);
            return;
        }

        // 注册到本实例会话管理器
        sessionManager.add(roomId, session);
        // 加入 Redis 在线集合（Set 去重，多端登录只算一个在线）
        redisUtils.sAdd(ChatConstants.REDIS_ONLINE_KEY_PREFIX + roomId, String.valueOf(userId));
        log.info("WebSocket 连接建立: roomId={}, userId={}, sessionId={}, 在线={}",
                roomId, userId, session.getId(),
                redisUtils.sSize(ChatConstants.REDIS_ONLINE_KEY_PREFIX + roomId));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 仅处理心跳：客户端发 "ping"，服务端回 "pong"；其他文本忽略（业务消息走 HTTP）
        String payload = message.getPayload();
        if ("ping".equalsIgnoreCase(payload)) {
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage("pong"));
                }
            } catch (Exception e) {
                log.warn("WebSocket 心跳响应失败: sessionId={}, msg={}", session.getId(), e.getMessage());
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Map<String, Object> attrs = session.getAttributes();
        Long userId = (Long) attrs.get("userId");
        Long roomId = (Long) attrs.get("roomId");

        // 从本实例会话管理器移除
        if (roomId != null) {
            sessionManager.remove(roomId, session);
        }
        // 从 Redis 在线集合移除（仅在用户该房间所有连接都断开时才真正移除）
        if (userId != null && roomId != null) {
            // 注意：同一用户可能多端登录有多个 session。
            // 这里简单按"本实例该房间连接数==0 且其他实例也没连接"判断较复杂，
            // 保守做法：本实例该房间连接数为 0 时移除该用户在线标记。
            // 多端场景下可能误移除（另一端还连着），后续如需精确可改为引用计数或保留 Set 由前端重连刷新。
            if (sessionManager.connectionCount(roomId) == 0) {
                redisUtils.sRemove(ChatConstants.REDIS_ONLINE_KEY_PREFIX + roomId, String.valueOf(userId));
            }
        }
        log.info("WebSocket 连接关闭: roomId={}, userId={}, sessionId={}, status={}",
                roomId, userId, session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket 传输错误: sessionId={}, msg={}", session.getId(), exception.getMessage());
        closeQuietly(session);
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (Exception ignored) {
        }
    }
}
