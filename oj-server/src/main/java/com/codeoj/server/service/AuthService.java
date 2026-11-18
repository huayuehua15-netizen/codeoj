package com.codeoj.server.service;

import com.codeoj.server.dto.LoginDTO;
import com.codeoj.server.dto.RegisterDTO;
import com.codeoj.server.security.LoginUser;
import com.codeoj.server.vo.LoginVO;

/**
 * 认证服务（论文表 3-3 用户与认证：注册、登录、登出、个人信息维护、JWT 认证）。
 */
public interface AuthService {

    /**
     * 注册：按客户端 IP 限流（防批量刷号）。
     *
     * <p>API.md §1.1 规定注册<b>不自动登录</b>、响应 {@code data: null}，
     * 因此这里不返回令牌，调用方也无从"顺手"拿到一个 JWT。
     */
    void register(RegisterDTO dto, String clientIp);

    /** 登录成功返回 JWT；连续失败 5 次锁定 15 分钟 + 按 IP 限流（论文 4.7） */
    LoginVO login(LoginDTO dto, String clientIp);

    /**
     * 登出：令牌加入 Redis 黑名单直至自然过期。
     *
     * @param token 裸 JWT（不含 {@code Bearer } 前缀），可为 null
     *              （例如请求未携带 Authorization 头时）
     */
    void logout(LoginUser user, String token);

    LoginVO getUserInfo(LoginUser user);

    /** 邮箱修改（论文表 3-1 个人信息维护） */
    void updateEmail(LoginUser user, String email);

    /**
     * 密码修改：验证旧密码，成功后由本方法把当前令牌拉黑使其立即失效。
     *
     * @param token 裸 JWT（不含 {@code Bearer } 前缀），可为 null
     */
    void changePassword(LoginUser user, String oldPassword, String newPassword, String token);
}
