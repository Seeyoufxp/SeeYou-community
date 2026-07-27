package com.seeyou.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.ai.autoconfigure.vectorstore.redis.RedisVectorStoreAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * AI 微服务
 * - 无 DB（仅依赖 Redis 缓存 + 向量库 + LLM/Embedding）
 * - 通过 OpenFeign 调用 user/content 内部接口
 * - 通过 RocketMQ Stream 消费 content-event-topic 同步向量知识库
 *
 * exclude RedisVectorStoreAutoConfiguration：在 AiConfig 手动配 JedisPooled + RedisVectorStore，
 * 原因——starter autoconfigure 通过 JedisConnectionFactory 转 JedisPooled 时 password 丢失（M4+Jedis5.0.2 兼容问题）。
 */
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.seeyou.ai")
@SpringBootApplication(scanBasePackages = "com.seeyou", exclude = {
    DataSourceAutoConfiguration.class,
    RedisVectorStoreAutoConfiguration.class
})
public class AiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}
