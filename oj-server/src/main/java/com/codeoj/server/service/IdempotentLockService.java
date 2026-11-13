package com.codeoj.server.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 重复提交幂等锁（论文表 4-6：oj:lock:submit:{userId} STRING(NX) TTL 2s）。
 */
@Component
public class IdempotentLockService {

    private final StringRedisTemplate redis;

    public IdempotentLockService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** SET NX EX：拿到锁返回 true，2 秒内重复提交被拦截 */
    public boolean tryLock(String key, int ttlSeconds) {
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(ttlSeconds)));
    }

    public void unlock(String key) {
        redis.delete(key);
    }
}
