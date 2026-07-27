package com.seeyou.ai.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 天气信息 DTO
 * 由 QWeatherService.getCurrentWeather 返回，供 AI 欢迎语 Function Calling 使用。
 * 字段值均为字符串（和风 API 原样返回 / 聚合），便于 LLM 直接消费。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherInfo implements Serializable {
    /** 城市名 */
    private String city;
    /** 当前小时温度（摄氏度） */
    private String temp;
    /** 当天最高温度（摄氏度） */
    private String tempMax;
    /** 当天最低温度（摄氏度） */
    private String tempMin;
    /** 天气现象文字描述（晴/多云/雨...） */
    private String text;
    /** 风向 */
    private String windDir;
    /** 风力等级 */
    private String windScale;
    /** 湿度（百分比） */
    private String humidity;
}
