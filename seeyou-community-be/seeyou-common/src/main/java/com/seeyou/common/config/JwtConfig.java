package com.seeyou.common.config;

import com.seeyou.common.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 自动配置
 * 所有依赖 seeyou-common 的服务自动获得 JwtUtils Bean
 * jwt.secret / jwt.expire 统一从 config/application-secret.yml 读取
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(JwtUtils.class)
public class JwtConfig {

    @Value("${jwt.secret:seeyou-community-default-secret-change-me}")
    private String secret;

    @Value("${jwt.expire:86400000}")
    private long expire;

    @Bean
    public JwtUtils jwtUtils() {
        return new JwtUtils(secret, expire);
    }
}
