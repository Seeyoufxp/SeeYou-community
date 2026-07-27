package com.seeyou.ai.service;

import com.seeyou.ai.pojo.vo.WelcomeVO;

/**
 * 欢迎信息服务
 * 当日用户首次访问时，由 AI 调用 Function Calling（注册时长+天气）生成欢迎语，存 Redis 当日有效。
 */
public interface WelcomeService {

    /**
     * 获取欢迎语
     * - 未登录：返回通用欢迎语（不调 LLM）
     * - 登录：优先读 Redis 缓存；未命中则走 ChatClient + Function Calling 生成，存 Redis 到次日零点
     */
    WelcomeVO getWelcome();
}
