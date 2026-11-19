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

    /**
     * 显式释放幂等锁。
     *
     * <p><b>当前无调用点，预留给需要显式释放的幂等场景</b>：提交幂等锁刻意依赖 TTL 自然过期
     * （2 秒），而不是提交成功后立刻删除——提前删除会让"同一秒内的第二次点击"绕过拦截，
     * 反而破坏幂等语义。保留该方法是为了将来出现"失败即放开重试"的场景（例如短时锁 +
     * 业务失败立即可重试）时不必再改接口。
     */
    public void unlock(String key) {
        redis.delete(key);
    }
}
