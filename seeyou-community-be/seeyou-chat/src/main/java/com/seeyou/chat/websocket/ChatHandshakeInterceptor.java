package com.seeyou.chat.websocket;

import com.seeyou.common.utils.JwtUtils;
import com.seeyou.common.utils.RedisUtils;
import com.seeyou.common.constant.CommonConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 握手拦截器：从 query 参数解析 token 和 roomId，校验 JWT。
 * <p>
 * 握手 URL 形如：ws://host/api/chat/ws?roomId=123&token=xxx
 * （WebSocket 无法自定义 header，故 token 走 query；网关白名单放行 /api/chat/ws/**，
 * 由本服务在握手时自行鉴权。）
 * <p>
 * 校验流程与网关 AuthGlobalFilter 保持一致：JWT 签名/exp → Redis 二次校验（踢人/登出失效）→ 滑动续期。
 * 校验通过则把 userId/username/roomId 放入 attributes（后续 Handler 可从 session.getAttributes() 取）；
 * 校验失败返回 401，拒绝握手。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtils jwtUtils;
    private final RedisUtils redisUtils;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        URI uri = request.getURI();
        String query = uri.getQuery() == null ? "" : uri.getQuery();
        Map<String, String> params = parseQuery(query);

        String token = params.get("token");
        String roomIdStr = params.get("roomId");

        // 参数校验
        if (token == null || token.isBlank() || roomIdStr == null || roomIdStr.isBlank()) {
            log.warn("WebSocket 握手失败：缺少 token 或 roomId, uri={}", uri);
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }

        // 去掉 Bearer 前缀（兼容客户端统一带前缀的场景）
        if (token.startsWith(CommonConstants.TOKEN_PREFIX)) {
            token = token.substring(CommonConstants.TOKEN_PREFIX.length());
        }

        // JWT 校验
        if (!jwtUtils.validate(token)) {
            log.warn("WebSocket 握手失败：token 无效或已过期, uri={}", uri);
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }

        Long userId = jwtUtils.getUserId(token);
        String username = jwtUtils.getUsername(token);
        if (userId == null) {
            log.warn("WebSocket 握手失败：token 解析 userId 为空, uri={}", uri);
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }

        // Redis 二次校验：与网关一致，拦截已登出/被踢/被封禁的 token
        String redisKey = CommonConstants.TOKEN_REDIS_PREFIX + userId;
        String cachedToken = redisUtils.get(redisKey);
        if (cachedToken == null || !cachedToken.equals(token)) {
            log.warn("WebSocket 握手失败：token 已失效（Redis 校验未通过）, userId={}, uri={}", userId, uri);
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }
        // 滑动续期（握手是活跃使用，与网关行为一致）
        redisUtils.expire(redisKey, CommonConstants.TOKEN_EXPIRE_MS, TimeUnit.MILLISECONDS);

        Long roomId;
        try {
            roomId = Long.parseLong(roomIdStr);
        } catch (NumberFormatException e) {
            log.warn("WebSocket 握手失败：roomId 非法, roomIdStr={}", roomIdStr);
            response.setStatusCode(org.springframework.http.HttpStatus.BAD_REQUEST);
            return false;
        }

        // 写入 attributes，Handler 可通过 session.getAttributes() 读取
        attributes.put("userId", userId);
        attributes.put("username", username);
        attributes.put("roomId", roomId);
        log.info("WebSocket 握手成功: userId={}, username={}, roomId={}", userId, username, roomId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手后无需处理；异常（非空）仅记录日志
        if (exception != null) {
            log.warn("WebSocket 握手异常", exception);
        }
    }

    /** 简单解析 query string（不依赖 servlet API） */
    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new java.util.HashMap<>();
        if (query.isEmpty()) {
            return params;
        }
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = pair.substring(0, idx);
                String value = pair.substring(idx + 1);
                try {
                    params.put(java.net.URLDecoder.decode(key, java.nio.charset.StandardCharsets.UTF_8),
                            java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8));
                } catch (Exception e) {
                    params.put(key, value);
                }
            }
        }
        return params;
    }
}
