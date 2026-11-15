package com.codeoj.server.controller;

import com.codeoj.common.result.R;
import com.codeoj.server.dto.LoginDTO;
import com.codeoj.server.dto.RegisterDTO;
import com.codeoj.server.security.JwtUtil;
import com.codeoj.server.security.LoginUser;
import com.codeoj.server.security.TokenBlacklistService;
import com.codeoj.server.service.AuthService;
import com.codeoj.server.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口（论文表 4-7）：注册/登录公开，登出将令牌加入 Redis 黑名单。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklistService;

    public AuthController(AuthService authService, JwtUtil jwtUtil,
                          TokenBlacklistService blacklistService) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.blacklistService = blacklistService;
    }

    @PostMapping("/register")
    public R<LoginVO> register(@Valid @RequestBody RegisterDTO dto, HttpServletRequest request) {
        return R.ok(authService.register(dto, clientIp(request)));
    }

    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        return R.ok(authService.login(dto, clientIp(request)));
    }

    /** 客户端 IP：优先代理头，回退直连地址（Nginx 反代场景，论文 4.2） */
    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/logout")
    public R<Void> logout(@AuthenticationPrincipal LoginUser user,
                          @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // 登出：令牌加入 Redis 黑名单直至自然过期（论文 4.7/5.2）
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            jwtUtil.tryParse(token).ifPresent(claims ->
                    blacklistService.add(claims.getId(), jwtUtil.remainingMs(token)));
        }
        authService.logout(user);
        return R.ok();
    }

    /** 当前用户信息（论文表 4-7 GET /api/auth/info） */
    @GetMapping("/info")
    public R<LoginVO> info(@AuthenticationPrincipal LoginUser user) {
        return R.ok(authService.getUserInfo(user));
    }
}
