package com.seeyou.ai.function;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 天气 Function 的返回值
 * LLM 拿到后用于在欢迎语中加入天气提示。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 城市名 */
    private String city;
    /** 当前小时温度（摄氏度） */
    private String temp;
    /** 当天最高温度（摄氏度） */
    private String tempMax;
    /** 当天最低温度（摄氏度） */
    private String tempMin;
    /** 天气现象（晴/多云/雨...） */
    private String text;
    /** 风向 */
    private String windDir;
    /** 湿度（百分比） */
    private String humidity;
}
