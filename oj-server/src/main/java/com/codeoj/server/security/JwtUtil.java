package com.codeoj.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * JWT 工具（论文 5.2）：登录校验 BCrypt 密码后签发携带用户标识的 JWT；
 * jti 用于登出黑名单键（oj:token:blacklist:{jti}）。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMs;

    public JwtUtil(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.expireMs = properties.getExpireHours() * 3600_000L;
    }

    /** 签发令牌：subject=userId，claim role，返回 [token, jti] */
    public String issue(Long userId, Integer role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expireMs)))
                .signWith(key)
                .compact();
    }

    /** 剩余有效毫秒数（黑名单 TTL 用） */
    public long remainingMs(String token) {
        Claims claims = parse(token);
        return Math.max(0, claims.getExpiration().getTime() - System.currentTimeMillis());
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }

    public Optional<Claims> tryParse(String token) {
        try {
            return Optional.of(parse(token));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
