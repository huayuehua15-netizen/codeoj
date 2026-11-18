package com.codeoj.server.controller;

import com.codeoj.common.result.R;
import com.codeoj.server.dto.ChangePasswordDTO;
import com.codeoj.server.dto.LoginDTO;
import com.codeoj.server.dto.RegisterDTO;
import com.codeoj.server.dto.UpdateEmailDTO;
import com.codeoj.server.security.LoginUser;
import com.codeoj.server.service.AuthService;
import com.codeoj.server.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证接口（论文表 4-7）：注册/登录公开，登出将令牌加入 Redis 黑名单。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 注册：成功返回 200（不签发令牌——注册后应引导用户走登录流程） */
    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody RegisterDTO dto, HttpServletRequest request) {
        authService.register(dto, clientIp(request));
        return R.ok();
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
        // 登出：controller 只负责把请求头里的裸 token 交给 service，
        // 拉黑逻辑（含 TTL 计算）属于登出语义本身，放在 service 里免得别的入口漏掉。
        authService.logout(user, bearerToken(authHeader));
        return R.ok();
    }

    /** 当前用户信息（论文表 4-7 GET /api/auth/info） */
    @GetMapping("/info")
    public R<LoginVO> info(@AuthenticationPrincipal LoginUser user) {
        return R.ok(authService.getUserInfo(user));
    }

    /** 邮箱修改（论文表 3-1 个人信息维护） */
    @PutMapping("/info")
    public R<Void> updateEmail(@Valid @RequestBody UpdateEmailDTO dto,
                               @AuthenticationPrincipal LoginUser user) {
        authService.updateEmail(user, dto.getEmail());
        return R.ok();
    }

    /** 签名修改（API.md v1.8 §1.5a）：trim 后 1-200 字 */
    @PutMapping("/profile")
    public R<Void> updateProfile(@RequestBody Map<String, String> body,
                                 @AuthenticationPrincipal LoginUser user) {
        authService.updateSignature(user, body.getOrDefault("signature", ""));
        return R.ok();
    }

    /** 头像上传（API.md v1.8 §1.5b）：data URL 进，落盘后返回相对 URL */
    @PutMapping("/avatar")
    public R<Map<String, String>> updateAvatar(@RequestBody Map<String, String> body,
                                               @AuthenticationPrincipal LoginUser user) {
        return R.ok(Map.of("avatar", authService.updateAvatar(user, body.get("dataUrl"))));
    }

    /** 密码修改：验证旧密码，成功后当前令牌立即失效（黑名单），前端跳转重新登录 */
    @PutMapping("/password")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto,
                                  @AuthenticationPrincipal LoginUser user,
                                  @RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.changePassword(user, dto.getOldPassword(), dto.getNewPassword(),
                bearerToken(authHeader));
        return R.ok();
    }

    /**
     * 从 Authorization 头取出裸 JWT（剥掉 {@code Bearer } 前缀）。
     * 头缺失或格式不符时返回 null——JwtUtil 签发时从不带前缀，
     * 把带前缀的字符串直接交给解析器只会解析失败。
     */
    private static String bearerToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
