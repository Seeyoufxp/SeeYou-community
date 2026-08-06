package com.seeyou.ai.service.impl;

import com.seeyou.ai.config.QWeatherProperties;
import com.seeyou.ai.pojo.dto.WeatherInfo;
import com.seeyou.ai.service.QWeatherService;
import com.seeyou.common.utils.RedisUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 和风天气服务实现
 * 订阅版自定义 API Host，调用 /geo/v2/city/lookup 查 locationId，/v7/weather/24h 查逐小时预报。
 *
 * 缓存策略：
 * - 城市名→locationId：Redis 7 天（稳定映射，不变）
 * - 24h 逐小时预报：Redis 1 小时（和风每小时更新一次预报，TTL 1h 既省调用又保新鲜）
 *
 * 取数逻辑：
 * - 从 24h 缓存/接口拿 hourly[24] → pickCurrentHour 找当前小时（找不到取第一条兜底）→ 当前温度/天气
 * - aggregateTodayMaxMin 聚合今天日期的小时 → 当天最高/最低温（今天小时不足 3 条时退化为 24h 全部 max/min）
 *
 * 响应是 gzip 压缩，使用注入的 RestClient.Builder（Spring Boot 自动配置）自动解压。任意异常 try-catch 降级返回 null，不抛异常。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QWeatherServiceImpl implements QWeatherService {

    private final QWeatherProperties properties;
    private final RedisUtils redisUtils;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    /** 默认城市（北京）locationId，用户城市为空或查找失败时兜底 */
    private static final String DEFAULT_LOCATION_ID = "101010100";
    private static final String DEFAULT_CITY_NAME = "北京";
    /** 城市名→locationId 缓存 key 前缀，TTL 7天 */
    private static final String CITY_ID_CACHE_PREFIX = "qweather:city:";
    private static final long CITY_ID_CACHE_DAYS = 7L;
    /** 24h 逐小时预报缓存前缀，TTL 1 小时（和风每小时更新一次预报） */
    private static final String WEATHER_24H_CACHE_PREFIX = "qweather:24h:";
    private static final long WEATHER_CACHE_HOURS = 1L;
    /** 中国时区，用于和风 fxTime（ISO 8601 带时区）小时/日期比较 */
    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");

    @Override
    public WeatherInfo getCurrentWeather(String cityName) {
        // 城市名为空时直接返回 null（用户没设地区时不要 fallback 北京）
        if (cityName == null || cityName.isBlank()) {
            log.debug("城市名为空，跳过天气查询");
            return null;
        }
        try {
            String locationId = resolveLocationId(cityName);
            if (locationId == null) {
                log.warn("解析 locationId 失败: city={}", cityName);
                return null;
            }
            JsonNode hourly = getHourly(locationId);
            if (hourly == null || !hourly.isArray() || hourly.isEmpty()) {
                log.warn("获取24h天气失败或为空: city={}", cityName);
                return null;
            }
            // 当前小时对应的预报；24h 通常从下一个整点开始，找不到当前小时时取第一条（即将到来的一小时）
            JsonNode target = pickCurrentHour(hourly);
            if (target == null) {
                target = hourly.get(0);
            }
            // 聚合当天最高/最低温
            String[] maxMin = aggregateTodayMaxMin(hourly);
            return new WeatherInfo(
                    cityName,
                    target.path("temp").asText(),
                    maxMin[0],
                    maxMin[1],
                    target.path("text").asText(),
                    target.path("windDir").asText(),
                    target.path("windScale").asText(),
                    target.path("humidity").asText()
            );
        } catch (Exception e) {
            log.warn("获取天气失败(降级返回null): city={}", cityName, e);
            return null;
        }
    }

    /**
     * 获取 24h 逐小时预报
     * 先查 Redis 缓存（TTL 1 小时）；未命中调 /v7/weather/24h 接口并把 hourly 存缓存。
     * 缓存反序列化失败或调接口失败均降级返回 null，由调用方处理。
     */
    private JsonNode getHourly(String locationId) {
        String cacheKey = WEATHER_24H_CACHE_PREFIX + locationId;
        String cached = redisUtils.get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readTree(cached);
            } catch (Exception e) {
                log.warn("24h缓存反序列化失败，回源调接口: locationId={}", locationId, e);
            }
        }
        try {
            JsonNode resp = restClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v7/weather/24h")
                            .queryParam("location", locationId)
                            .queryParam("key", properties.getApiKey())
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            if (resp == null || !"200".equals(resp.path("code").asText())) {
                log.warn("和风24h接口返回非200: locationId={}, code={}", locationId,
                        resp == null ? "null" : resp.path("code").asText());
                return null;
            }
            JsonNode hourly = resp.path("hourly");
            if (hourly.isArray() && !hourly.isEmpty()) {
                try {
                    redisUtils.set(cacheKey, hourly.toString(), WEATHER_CACHE_HOURS, TimeUnit.HOURS);
                } catch (Exception e) {
                    log.warn("24h缓存写入失败(不影响主流程): locationId={}", locationId, e);
                }
            }
            return hourly;
        } catch (Exception e) {
            log.warn("调24h天气接口失败: locationId={}", locationId, e);
            return null;
        }
    }

    /**
     * 从 hourly 里找当前小时对应的那条
     * fxTime 格式如 "2026-07-27T11:00+08:00"（ISO 8601 带时区），统一转中国时区后比较小时。
     * 24h 通常从下一个整点开始（不含当前已过小时），找不到时返回 null，由调用方取第一条兜底。
     */
    private JsonNode pickCurrentHour(JsonNode hourly) {
        int currentHour = ZonedDateTime.now(CHINA_ZONE).getHour();
        for (JsonNode h : hourly) {
            String fxTime = h.path("fxTime").asText();
            try {
                OffsetDateTime odt = OffsetDateTime.parse(fxTime);
                if (odt.atZoneSameInstant(CHINA_ZONE).getHour() == currentHour) {
                    return h;
                }
            } catch (Exception e) {
                // fxTime 解析失败跳过该条
            }
        }
        return null;
    }

    /**
     * 聚合当天最高/最低温
     * 24h 可能跨天，只取"今天日期"的小时算 max/min；若今天小时不足 3 条（如深夜调用，今天已过大部分），
     * 退化为取 24h 全部的 max/min 作为近似。
     * @return [max, min]，无法计算时为 [null, null]
     */
    private String[] aggregateTodayMaxMin(JsonNode hourly) {
        int todayMax = Integer.MIN_VALUE;
        int todayMin = Integer.MAX_VALUE;
        int todayCount = 0;
        int allMax = Integer.MIN_VALUE;
        int allMin = Integer.MAX_VALUE;
        int allCount = 0;
        LocalDate today = LocalDate.now(CHINA_ZONE);
        for (JsonNode h : hourly) {
            try {
                int temp = Integer.parseInt(h.path("temp").asText());
                allMax = Math.max(allMax, temp);
                allMin = Math.min(allMin, temp);
                allCount++;
                OffsetDateTime odt = OffsetDateTime.parse(h.path("fxTime").asText());
                if (odt.atZoneSameInstant(CHINA_ZONE).toLocalDate().equals(today)) {
                    todayMax = Math.max(todayMax, temp);
                    todayMin = Math.min(todayMin, temp);
                    todayCount++;
                }
            } catch (Exception e) {
                // temp 解析失败跳过该条
            }
        }
        if (todayCount >= 3) {
            return new String[]{String.valueOf(todayMax), String.valueOf(todayMin)};
        }
        if (allCount > 0) {
            return new String[]{String.valueOf(allMax), String.valueOf(allMin)};
        }
        return new String[]{null, null};
    }

    /**
     * 城市名 → locationId
     * 先查 Redis 缓存；未命中调 geo 接口查找并缓存。
     * 城市名为空或查找失败，返回默认城市（北京）。
     */
    private String resolveLocationId(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return DEFAULT_LOCATION_ID;
        }
        String cacheKey = CITY_ID_CACHE_PREFIX + cityName;
        String cached = redisUtils.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        try {
            JsonNode geoResp = restClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/geo/v2/city/lookup")
                            .queryParam("location", cityName)
                            .queryParam("key", properties.getApiKey())
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            if (geoResp != null && "200".equals(geoResp.path("code").asText())) {
                JsonNode first = geoResp.path("location").path(0);
                String id = first.path("id").asText();
                if (!id.isBlank()) {
                    redisUtils.set(cacheKey, id, CITY_ID_CACHE_DAYS, TimeUnit.DAYS);
                    return id;
                }
            }
        } catch (Exception e) {
            log.warn("城市ID查找失败，回退默认城市: city={}", cityName, e);
        }
        return DEFAULT_LOCATION_ID;
    }

    private RestClient restClient() {
        // 使用 Spring Boot 自动配置的 RestClient.Builder，支持 gzip 自动解压
        return restClientBuilder.baseUrl(properties.getHost()).build();
    }
}
