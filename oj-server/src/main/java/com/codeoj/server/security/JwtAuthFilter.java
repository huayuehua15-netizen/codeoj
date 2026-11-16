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
                // 每请求主键查库：同时校验存在性、封禁状态与**当前角色**
                User u = userMapper.selectById(Long.valueOf(claims.getSubject()));
                if (u == null || (u.getStatus() != null && u.getStatus() == 1)) {
                    log.debug("token for deleted/banned user {}, reject", claims.getSubject());
                    return;
                }
                // 角色以数据库为准，不信令牌里的 role claim——
                // 否则管理员降权后，旧令牌在 24h 有效期内仍持 ROLE_ADMIN（垂直越权）
                LoginUser user = new LoginUser(u.getId(), u.getRole(), jti);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null,
                                List.of(new SimpleGrantedAuthority(
                                        "ROLE_" + (u.getRole() != null && u.getRole() == 1 ? "ADMIN" : "USER"))));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        chain.doFilter(request, response);
    }
}
