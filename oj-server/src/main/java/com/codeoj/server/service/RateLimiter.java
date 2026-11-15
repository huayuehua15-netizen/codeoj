package com.codeoj.server.service;

import com.codeoj.common.exception.BizException;
import com.codeoj.common.result.ResultCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis 固定窗口限流（论文 4.7/5.2）：注册、登录与提交接口均以 Redis 计数实现滑动窗口限流。
 * 键：oj:rate:{type}:{key}，窗口内计数超限则抛业务异常。
 * 计数与过期由 Lua 脚本原子执行，避免 INCR 与 EXPIRE 之间进程中断产生永不过期的计数键。
 */
@Component
public class RateLimiter {

    /** 原子计数：首次访问设置过期，返回当前计数 */
    private static final DefaultRedisScript<Long> INCR_WITH_TTL = new DefaultRedisScript<>(
            "local c = redis.call('INCR', KEYS[1]) " +
            "if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " +
            "return c", Long.class);

    private final StringRedisTemplate redis;

    public RateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 检查并计数：窗口 windowSeconds 内最多 max 次，超限抛 RATE_LIMITED。
     *
     * @param type 限流类型（register/login/submit 等）
     * @param key  限流主体（如客户端 IP、userId）
     */
    public void check(String type, String key, int max, int windowSeconds) {
        String redisKey = "oj:rate:" + type + ":" + key;
        Long count = redis.execute(INCR_WITH_TTL, List.of(redisKey), String.valueOf(windowSeconds));
        if (count != null && count > max) {
            throw new BizException(ResultCode.RATE_LIMITED);
        }
    }
}
