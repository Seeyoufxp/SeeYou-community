package com.seeyou.common.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Redis 通用操作工具类
 * 通过 AutoConfiguration 注册，业务侧可直接 @Autowired RedisUtils 使用
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(StringRedisTemplate.class)
@RequiredArgsConstructor
public class RedisUtils {

    private final StringRedisTemplate stringRedisTemplate;

    public void set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, String value, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    public Boolean delete(String key) {
        return stringRedisTemplate.delete(key);
    }

    public Boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return stringRedisTemplate.expire(key, timeout, unit);
    }

    public Long increment(String key, long delta) {
        return stringRedisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * setIfAbsent：用于简易分布式锁（SET key value NX EX）
     * @return true 表示获取锁成功
     */
    public Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
        return stringRedisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
    }

    // ============== Set 操作（用于点赞关系集合） ==============

    /**
     * 向 Set 添加元素
     * @return 新增元素数量（0 表示已存在，1 表示新增）
     */
    public Long sAdd(String key, String... values) {
        return stringRedisTemplate.opsForSet().add(key, values);
    }

    /**
     * 从 Set 移除元素
     * @return 实际移除数量
     */
    public Long sRemove(String key, Object... values) {
        return stringRedisTemplate.opsForSet().remove(key, values);
    }

    /**
     * 判断元素是否在 Set 中
     */
    public Boolean sIsMember(String key, Object value) {
        return stringRedisTemplate.opsForSet().isMember(key, value);
    }

    /**
     * 获取 Set 大小
     */
    public Long sSize(String key) {
        return stringRedisTemplate.opsForSet().size(key);
    }
}
