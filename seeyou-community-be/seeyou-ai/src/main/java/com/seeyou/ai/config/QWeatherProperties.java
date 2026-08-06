package com.seeyou.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 和风天气配置
 * 订阅版自定义 API Host，配置在 application-secret.yml 的 qweather.*
 */
@Data
@ConfigurationProperties(prefix = "qweather")
public class QWeatherProperties {

    /** API Host */
    private String host;

    /** API Key */
    private String apiKey;
}
