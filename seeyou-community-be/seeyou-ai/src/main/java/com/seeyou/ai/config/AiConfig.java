package com.seeyou.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.autoconfigure.vectorstore.redis.RedisVectorStoreProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.RedisVectorStore;
import org.springframework.ai.vectorstore.RedisVectorStore.RedisVectorStoreConfig;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

/**
 * AI 模块配置
 * - ChatClient：默认不绑定 system message 和 function，由各业务场景按需注入
 * - JedisPooled + RedisVectorStore：手动配，替代 RedisVectorStoreAutoConfiguration。
 *   原因：starter autoconfigure 通过 JedisConnectionFactory 转 JedisPooled 时 password 丢失
 *   （Spring AI 1.0.0-M4 + Jedis 5.0.2 兼容问题），手动用 JedisPooled(host,port,password) 构造
 *   确保 password 生效。RedisVectorStore 用 builder 配 config，从 RedisVectorStoreProperties 读
 *   index/prefix/initializeSchema。
 * - VectorStore：业务代码（AssistantService / KnowledgeService）面向 VectorStore 接口编程。
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({QWeatherProperties.class, KnowledgeProperties.class, RedisVectorStoreProperties.class})
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    /**
     * 手动创建 JedisPooled（带 password），给 RedisVectorStore 用。
     * JedisPooled(host, port, password) 是 Jedis 客户端直连 Redis 的统一客户端。
     */
    @Bean(destroyMethod = "close")
    public JedisPooled jedisPooled(RedisProperties redisProperties) {
        String host = redisProperties.getHost();
        int port = redisProperties.getPort();
        String password = redisProperties.getPassword();
        log.info("手动配置 JedisPooled: host={}, port={}, passwordNull={}", host, port, password == null);
        if (password != null && !password.isEmpty()) {
            // Jedis 5.x API：JedisPooled(host, port, user, password)；Redis requirepass 等价于 ACL default user
            return new JedisPooled(host, port, "default", password);
        }
        return new JedisPooled(host, port);
    }

    /**
     * 手动创建 RedisVectorStore（builder 配 config + 注入 EmbeddingModel 和带密码的 JedisPooled）
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel,
                                   RedisVectorStoreProperties properties,
                                   JedisPooled jedisPooled) {
        RedisVectorStoreConfig config = RedisVectorStoreConfig.builder()
                .withIndexName(properties.getIndex())
                .withPrefix(properties.getPrefix())
                .withContentFieldName("content")
                .withEmbeddingFieldName("embedding")
                .withVectorAlgorithm(RedisVectorStore.Algorithm.HSNW)
                .build();
        log.info("手动配置 RedisVectorStore: index={}, prefix={}, initializeSchema={}",
                properties.getIndex(), properties.getPrefix(), properties.isInitializeSchema());
        return new RedisVectorStore(config, embeddingModel, jedisPooled, properties.isInitializeSchema());
    }
}