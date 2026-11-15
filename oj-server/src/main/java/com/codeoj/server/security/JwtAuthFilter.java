package com.codeoj.server.security;

import com.codeoj.common.entity.User;
import com.codeoj.server.mapper.UserMapper;
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
 * 黑名单中的令牌直接拒绝；已删除账号的令牌经存在性校验立即统一失效
 * （JWT 无状态，删号不会自动作废已签发令牌，此处每请求按 userId 主键查库校验）。
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklistService;
    private final UserMapper userMapper;

    public JwtAuthFilter(JwtUtil jwtUtil,
                         TokenBlacklistService blacklistService,
                         UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.blacklistService = blacklistService;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // Tomcat 线程池复用 ThreadLocal：每请求先清空安全上下文，
        // 防止"拒绝注入"的请求（黑名单/已删号）读到上一请求残留的认证
        SecurityContextHolder.clearContext();

        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            jwtUtil.tryParse(token).ifPresent(claims -> {
                String jti = claims.getId();
                if (blacklistService.contains(jti)) {
                    log.debug("token in blacklist, jti={}", jti);
                    return;
                }
                LoginUser user = buildLoginUser(claims);
                if (!userExists(user.userId())) {
                    // 账号已删除：不注入认证，受保护接口统一返回 40100（行为一致）
                    log.debug("token for deleted user {}, reject", user.userId());
                    return;
                }
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + roleOf(claims))));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        chain.doFilter(request, response);
    }

    /**
     * 用户存在性校验：每请求主键查库（主键查询代价极小），保证删号后 token **立即**全面失效、
     * 各接口行为一致。此前用 Redis 缓存 5 分钟的方案存在删号前已建立的缓存窗口，
     * 窗口内行为不一致，故弃用。
     */
    private boolean userExists(Long userId) {
        return userMapper.selectById(userId) != null;
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
