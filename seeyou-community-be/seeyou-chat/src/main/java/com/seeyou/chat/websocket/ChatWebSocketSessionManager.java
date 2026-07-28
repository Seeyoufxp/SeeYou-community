package com.seeyou.chat.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话管理器：维护 roomId -> 该房间所有连接的 session 集合。
 * 线程安全：每个房间的 session 集合用 ConcurrentHashMap.newKeySet()。
 * 用于 MQ 消费后向本实例连接的客户端广播消息。
 */
@Slf4j
@Component
public class ChatWebSocketSessionManager {

    /** roomId -> Set<WebSocketSession> */
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    /** 注册 session 到对应房间 */
    public void add(Long roomId, WebSocketSession session) {
        if (roomId == null || session == null) {
            return;
        }
        roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("WebSocket session 加入房间: roomId={}, sessionId={}, 房间连接数={}",
                roomId, session.getId(), roomSessions.get(roomId).size());
    }

    /** 移除 session（从其所属房间） */
    public void remove(Long roomId, WebSocketSession session) {
        if (roomId == null || session == null) {
            return;
        }
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                roomSessions.remove(roomId);
            }
        }
        log.debug("WebSocket session 离开房间: roomId={}, sessionId={}", roomId, session.getId());
    }

    /** 向指定房间的所有在线 session 广播文本消息 */
    public void send(Long roomId, String json) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions == null || sessions.isEmpty()) {
            // 本实例该房间无连接，正常情况（消息由其他实例推送）
            return;
        }
        TextMessage textMessage = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                // 同步发送，synchronized 防止 Spring WebSocket 并发写同一 session 报错
                synchronized (session) {
                    session.sendMessage(textMessage);
                }
            } catch (IOException e) {
                log.warn("WebSocket 推送失败: roomId={}, sessionId={}, msg={}",
                        roomId, session.getId(), e.getMessage());
            }
        }
    }

    /** 某房间当前连接数（本实例） */
    public int connectionCount(Long roomId) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        return sessions == null ? 0 : sessions.size();
    }
}
