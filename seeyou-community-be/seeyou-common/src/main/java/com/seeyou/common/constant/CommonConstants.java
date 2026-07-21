package com.seeyou.common.constant;

/**
 * 公共常量
 */
public class CommonConstants {

    /** 请求头中携带的 token */
    public static final String HEADER_TOKEN = "Authorization";

    /** token 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** 网关透传给下游的用户ID请求头 */
    public static final String HEADER_USER_ID = "X-User-Id";

    /** 网关透传给下游的用户名请求头 */
    public static final String HEADER_USERNAME = "X-Username";

    /** 网关透传给下游的用户角色请求头 */
    public static final String HEADER_USER_ROLE = "X-User-Role";

    /** Redis token key 前缀 */
    public static final String TOKEN_REDIS_PREFIX = "token:";

    /** token 默认过期时间（毫秒），24h */
    public static final long TOKEN_EXPIRE_MS = 24 * 60 * 60 * 1000L;

    private CommonConstants() {
    }
}
