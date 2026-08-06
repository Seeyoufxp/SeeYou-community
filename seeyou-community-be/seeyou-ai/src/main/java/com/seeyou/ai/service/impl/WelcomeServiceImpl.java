package com.seeyou.ai.service.impl;

import com.seeyou.ai.pojo.vo.WelcomeVO;
import com.seeyou.ai.service.WelcomeService;
import com.seeyou.common.context.UserContext;
import com.seeyou.common.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 欢迎信息服务实现
 * - 未登录返回固定欢迎语，不消耗 LLM 配额
 * - 登录用户：Redis key=ai:welcome:{userId}:{date} 缓存当日有效，命中直接返回；
 *   未命中走 ChatClient + 两个 Function（getUserRegisterInfo / getCurrentWeather）生成欢迎语
 *
 * LLM 调用失败时降级返回通用欢迎语，不抛异常（欢迎语是增强体验，失败不应阻断首页加载）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WelcomeServiceImpl implements WelcomeService {

    private final ChatClient chatClient;
    private final RedisUtils redisUtils;

    private static final String WELCOME_CACHE_PREFIX = "ai:welcome:";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String DEFAULT_GUEST_WELCOME =
            "欢迎来到之友开发者社区，登录后可体验个性化AI开发助手。";

    private static final String SYSTEM_PROMPT =
            "你是之友(SeeYou)开发者社区的欢迎助手。请根据当前用户的注册时长和所在城市天气，" +
            "生成一句简短温暖的欢迎语（不超过 50 字），语气友好自然，要嘘寒问暖（关心用户冷热）。" +
            "天气处理规则：当前温度较高（接近当天最高温）时，报最高温并提醒防暑防晒；" +
            "当前温度较低（接近当天最低温）时，报最低温并提醒添衣保暖；" +
            "温度适中时不报具体温度，用\"今天天气不错\"\"适合外出\"等概括性描述。" +
            "你可以调用 getUserRegisterInfo 获取用户注册信息，调用 getCurrentWeather 获取天气（含当前温度、当天最高/最低温）。" +
            "**如果用户没有设置所在城市（getUserRegisterInfo 返回的 city 为空/未设置），" +
            "getCurrentWeather 会返回 null，此时请勿提及任何天气、气温、地区相关内容，只根据注册天数生成通用欢迎语。**" +
            "**绝对不要 fallback 到某个默认城市（如\"北京\"），用户没填就是没填。**" +
            "不要提及具体时间（如\"现在是几点\"）。" +
            "若函数返回 null（未登录或获取失败），则生成通用欢迎语，不要提及具体天数或天气。" +
            "输出格式要求：只输出一句话，不要换行；" +
            "全篇最多使用一个 emoji 表情（也可以不用），若使用则放在句尾。";

    @Override
    public WelcomeVO getWelcome() {
        String today = LocalDate.now().format(DATE_FMT);
        Long userId = UserContext.getUserId();

        // 未登录：直接返回通用欢迎语，不调 LLM 节省成本
        if (userId == null) {
            return new WelcomeVO(DEFAULT_GUEST_WELCOME, today);
        }

        // 登录：先查 Redis 当日缓存
        String cacheKey = WELCOME_CACHE_PREFIX + userId + ":" + today;
        String cached = redisUtils.get(cacheKey);
        if (cached != null) {
            return new WelcomeVO(cached, today);
        }

        // 缓存未命中：走 ChatClient + Function Calling 生成
        String message;
        try {
            message = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user("请为当前登录用户生成今日(" + today + ")的欢迎语。")
                    .functions("getUserRegisterInfo", "getCurrentWeather")
                    .call()
                    .content();
            if (message == null || message.isBlank()) {
                message = "欢迎回到之友开发者社区，今天也要元气满满哦！";
            }
        } catch (Exception e) {
            log.warn("AI 生成欢迎语失败，降级返回通用欢迎语: userId={}", userId, e);
            message = "欢迎回到之友开发者社区，今天也要元气满满哦！";
        }

        // 存 Redis，TTL 到次日零点
        long secondsToMidnight = secondsToMidnight();
        redisUtils.set(cacheKey, message, secondsToMidnight, TimeUnit.SECONDS);
        log.info("生成欢迎语并缓存: userId={}, ttl={}s", userId, secondsToMidnight);
        return new WelcomeVO(message, today);
    }

    /** 计算当前时间到次日零点的秒数（至少 1 秒，避免 TTL=0） */
    private long secondsToMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrowMidnight = now.toLocalDate().plusDays(1).atStartOfDay();
        long seconds = Duration.between(now, tomorrowMidnight).getSeconds();
        return Math.max(seconds, 1L);
    }
}
