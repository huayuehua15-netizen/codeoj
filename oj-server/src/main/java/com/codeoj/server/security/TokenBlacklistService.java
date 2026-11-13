package com.codeoj.server.security;

import com.codeoj.common.constant.RedisKeys;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 登出令牌黑名单（论文 4.7/表 4-6）：登出将 jti 加入 Redis 黑名单直至令牌自然过期。
 */
@Component
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;

    public TokenBlacklistService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void add(String jti, long remainingMs) {
        if (remainingMs <= 0) {
            return;
        }
        redisTemplate.opsForValue()
                .set(RedisKeys.tokenBlacklist(jti), "1", Duration.ofMillis(remainingMs));
    }

    public boolean contains(String jti) {
        if (jti == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeys.tokenBlacklist(jti)));
    }
}
