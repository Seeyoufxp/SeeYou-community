package com.seeyou.common.utils;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSignerUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类（基于 Hutool 实现）
 * 网关与下游服务共用同一套 secret / 过期时间配置
 */
@Slf4j
public class JwtUtils {

    /** 默认过期时间：24 小时 */
    private static final long DEFAULT_EXPIRE = 24 * 60 * 60 * 1000L;

    private final String secret;
    private final long expire;

    public JwtUtils(String secret) {
        this(secret, DEFAULT_EXPIRE);
    }

    public JwtUtils(String secret, long expire) {
        this.secret = secret;
        this.expire = expire;
    }

    /**
     * 生成 token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param role      角色 0:普通用户 1:社区管理员 2:系统管理员
     */
    public String createToken(Long userId, String username, Integer role) {
        Map<String, Object> payload = new HashMap<>(5);
        payload.put("userId", userId);
        payload.put("username", username);
        payload.put("role", role);
        payload.put("iat", new Date());
        payload.put("exp", new Date(System.currentTimeMillis() + expire));
        return JWTUtil.createToken(payload, secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 校验 token 是否合法（签名 + 过期时间）
     */
    public boolean validate(String token) {
        try {
            JWT jwt = JWTUtil.parseToken(token).setSigner(JWTSignerUtil.hs256(secret.getBytes(StandardCharsets.UTF_8)));
            JWTValidator.of(jwt).validateDate();
            return jwt.verify();
        } catch (Exception e) {
            log.warn("JWT 校验失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从 token 中解析 userId
     */
    public Long getUserId(String token) {
        Object val = JWTUtil.parseToken(token).getPayload("userId");
        return val == null ? null : Long.valueOf(val.toString());
    }

    /**
     * 从 token 中解析 username
     */
    public String getUsername(String token) {
        Object val = JWTUtil.parseToken(token).getPayload("username");
        return val == null ? null : val.toString();
    }

    /**
     * 从 token 中解析 role
     */
    public Integer getRole(String token) {
        Object val = JWTUtil.parseToken(token).getPayload("role");
        return val == null ? null : Integer.valueOf(val.toString());
    }
}
