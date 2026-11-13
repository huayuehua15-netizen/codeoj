package com.codeoj.server.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器（论文 5.2）：在过滤器链中解析令牌并注入安全上下文；
 * 黑名单中的令牌直接拒绝；WS 握手请求不处理。
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklistService;

    public JwtAuthFilter(JwtUtil jwtUtil, TokenBlacklistService blacklistService) {
        this.jwtUtil = jwtUtil;
        this.blacklistService = blacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            jwtUtil.tryParse(token).ifPresentOrElse(claims -> {
                String jti = claims.getId();
                if (blacklistService.contains(jti)) {
                    log.debug("token in blacklist, jti={}", jti);
                    return;
                }
                LoginUser user = buildLoginUser(claims);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + roleOf(claims))));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }, () -> log.debug("invalid token"));
        }
        chain.doFilter(request, response);
    }

    private LoginUser buildLoginUser(Claims claims) {
        return new LoginUser(Long.valueOf(claims.getSubject()),
                (Integer) claims.get("role"), claims.getId());
    }

    private String roleOf(Claims claims) {
        Object role = claims.get("role");
        return role != null && ((Number) role).intValue() == 1 ? "ADMIN" : "USER";
    }
}
