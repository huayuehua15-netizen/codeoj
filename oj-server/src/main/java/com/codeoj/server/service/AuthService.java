package com.codeoj.server.service;

import com.codeoj.server.dto.LoginDTO;
import com.codeoj.server.dto.RegisterDTO;
import com.codeoj.server.security.LoginUser;
import com.codeoj.server.vo.LoginVO;

/**
 * 认证服务（论文表 3-3 用户与认证：注册、登录、登出、个人信息维护、JWT 认证）。
 */
public interface AuthService {

    LoginVO register(RegisterDTO dto);

    /** 登录成功返回 JWT；连续失败 5 次锁定 15 分钟（论文 4.7） */
    LoginVO login(LoginDTO dto);

    /** 登出：令牌加入 Redis 黑名单直至自然过期 */
    void logout(LoginUser user);

    LoginVO getUserInfo(LoginUser user);
}
