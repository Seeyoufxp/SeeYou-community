package com.seeyou.chat.config;

import com.seeyou.chat.websocket.ChatHandshakeInterceptor;
import com.seeyou.chat.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置。
 * <p>
 * 路径 /api/chat/ws（roomId/token 走 query 参数）。
 * 网关路由 /api/chat/** 转发到本服务时保留原始路径，故本服务注册路径与对外路径一致。
 * 跨域：允许所有源（WebSocket 握手鉴权由 ChatHandshakeInterceptor 完成）。
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ChatHandshakeInterceptor chatHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/api/chat/ws")
                .addInterceptors(chatHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
