package com.codeoj.server.service;

import com.codeoj.common.exception.BizException;
import com.codeoj.common.result.ResultCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 滑动窗口限流（论文 4.7/5.2）：登录与提交接口均以 Redis 计数实现滑动窗口限流。
 * 键：oj:rate:{type}:{key}，窗口内计数超限则抛业务异常。
 */
@Component
public class RateLimiter {

    private final StringRedisTemplate redis;

    public RateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 检查并计数：窗口 windowSeconds 内最多 max 次，超限抛 RATE_LIMITED。
     *
     * @param type 限流类型（login/submit 等）
     * @param key  限流主体（如 userId）
     */
    public void check(String type, String key, int max, int windowSeconds) {
        String redisKey = "oj:rate:" + type + ":" + key;
        Long count = redis.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            redis.expire(redisKey, Duration.ofSeconds(windowSeconds));
        }
        if (count != null && count > max) {
            throw new BizException(ResultCode.RATE_LIMITED);
        }
    }
}
