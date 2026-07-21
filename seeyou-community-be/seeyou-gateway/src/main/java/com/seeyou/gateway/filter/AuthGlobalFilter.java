package com.seeyou.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.seeyou.common.constant.CommonConstants;
import com.seeyou.common.utils.JwtUtils;
import com.seeyou.common.utils.RedisUtils;
import com.seeyou.gateway.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 全局鉴权过滤器
 * 1. 白名单请求直接放行
 * 2. 非白名单请求解析 Authorization header，校验 JWT
 * 3. 校验通过后将 userId/username/role 写入下游请求头
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final AuthProperties authProperties;
    private final JwtUtils jwtUtils;
    private final RedisUtils redisUtils;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        String token = exchange.getRequest().getHeaders().getFirst(CommonConstants.HEADER_TOKEN);
        if (StrUtil.isBlank(token)) {
            return unauthorized(exchange, "未登录");
        }

        // 支持 Bearer 前缀
        if (token.startsWith(CommonConstants.TOKEN_PREFIX)) {
            token = token.substring(CommonConstants.TOKEN_PREFIX.length());
        }

        if (!jwtUtils.validate(token)) {
            return unauthorized(exchange, "token 无效或已过期");
        }

        Long userId = jwtUtils.getUserId(token);
        String username = jwtUtils.getUsername(token);
        Integer role = jwtUtils.getRole(token);

        // Redis 二次校验：token 是否被登出/踢人/封禁等操作清除
        String redisKey = CommonConstants.TOKEN_REDIS_PREFIX + userId;
        String cachedToken = redisUtils.get(redisKey);
        if (cachedToken == null || !cachedToken.equals(token)) {
            return unauthorized(exchange, "token 已失效，请重新登录");
        }
        // 滑动续期
        redisUtils.expire(redisKey, CommonConstants.TOKEN_EXPIRE_MS, TimeUnit.MILLISECONDS);

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(CommonConstants.HEADER_USER_ID, String.valueOf(userId))
                .header(CommonConstants.HEADER_USERNAME, username == null ? "" : username)
                .header(CommonConstants.HEADER_USER_ROLE, role == null ? "0" : String.valueOf(role))
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isWhiteListed(String path) {
        List<String> whiteList = authProperties.getWhiteList();
        if (whiteList == null) {
            return false;
        }
        return whiteList.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"code\":401,\"message\":\"%s\"}", message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8))));
    }

    @Override
    public int getOrder() {
        // 优先级高于路由转发
        return -100;
    }
}
