package com.seeyou.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关白名单配置
 * 未匹配到白名单的请求将校验 token
 */
@Data
@Component
@ConfigurationProperties(prefix = "gateway.auth")
public class AuthProperties {

    /** 白名单路径（支持 ant 风格通配符） */
    private List<String> whiteList = new ArrayList<>(List.of(
            "/api/user/login",
            "/api/user/register",
            "/api/user/captcha",
            "/api/search/**",
            "/api/post/list",
            "/api/blog/list",
            "/api/qa/list",
            // 详情对未登录开放（单层通配，不包含 /like /unlike 等子操作）
            "/api/post/*",
            "/api/blog/*",
            "/api/qa/*",
            // 评论列表对未登录开放
            "/api/comment/list",
            "/api/ai/welcome",
            "/doc.html",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/favicon.ico"
    ));
}
