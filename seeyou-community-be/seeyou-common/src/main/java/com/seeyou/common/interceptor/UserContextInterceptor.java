package com.seeyou.common.interceptor;

import cn.hutool.core.util.StrUtil;
import com.seeyou.common.constant.CommonConstants;
import com.seeyou.common.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户上下文拦截器
 * 从网关透传的 X-User-Id / X-Username / X-User-Role Header 解析登录用户，
 * 写入 UserContext（ThreadLocal），请求结束后清理。
 */
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userIdHeader = request.getHeader(CommonConstants.HEADER_USER_ID);
        String usernameHeader = request.getHeader(CommonConstants.HEADER_USERNAME);
        String roleHeader = request.getHeader(CommonConstants.HEADER_USER_ROLE);

        if (StrUtil.isNotBlank(userIdHeader)) {
            try {
                Long userId = Long.valueOf(userIdHeader);
                Integer role = StrUtil.isNotBlank(roleHeader) ? Integer.valueOf(roleHeader) : 0;
                UserContext.set(new UserContext.LoginUser(userId, usernameHeader, role));
            } catch (NumberFormatException ignored) {
                // 非法 userId 头，视为未登录，不写入上下文
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 必须清理，防止线程池复用导致数据串号
        UserContext.clear();
    }
}
