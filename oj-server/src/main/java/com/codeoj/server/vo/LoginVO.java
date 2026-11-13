package com.codeoj.server.vo;

import com.codeoj.common.entity.User;
import lombok.Builder;
import lombok.Data;

/**
 * 登录/注册响应：返回 JWT 与用户信息。
 */
@Data
@Builder
public class LoginVO {

    private String token;
    private Long userId;
    private String username;
    private String nickname;
    /** 0=用户，1=管理员 */
    private Integer role;

    public static LoginVO of(String token, User user) {
        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole())
                .build();
    }
}
