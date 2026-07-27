package com.seeyou.ai.service;

import com.seeyou.ai.pojo.dto.WeatherInfo;

/**
 * 和风天气服务
 * 提供城市名 → 实时天气查询，供 AI 欢迎语 Function Calling 使用。
 * 城市名→locationId 的映射缓存到 Redis（7天），避免重复调 geo 接口。
 */
public interface QWeatherService {

    /**
     * 按城市名获取实时天气
     * 城市名为空或查找失败时，回退到默认城市（北京）。
     * 任意异常均降级返回 null，由调用方决定如何处理（不阻断欢迎语生成）。
     *
     * @param cityName 城市名（如"北京"、"上海"），可为空
     * @return 天气信息；获取失败返回 null
     */
    WeatherInfo getCurrentWeather(String cityName);
}
