package com.codeoj.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

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
            // 用 debug 而不是 warn：JwtAuthFilter 对每个请求都会走一次 tryParse，
            // 匿名请求与过期令牌都是正常流量，warn 会把它们刷成告警噪音，
            // 真正需要排查签名/过期问题时把 logging.level.com.codeoj 调成 debug 即可。
            log.debug("jwt parse failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
