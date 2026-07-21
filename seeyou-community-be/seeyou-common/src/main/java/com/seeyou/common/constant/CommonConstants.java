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

    private CommonConstants() {
    }
}
