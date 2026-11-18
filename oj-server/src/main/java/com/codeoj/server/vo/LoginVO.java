package com.codeoj.server.vo;

import com.codeoj.common.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录/注册/用户信息响应：返回 JWT 与用户资料。
 */
@Data
@Builder
public class LoginVO {

    private String token;
    private Long userId;
    private String username;
    private String email;
    /** 0=用户，1=管理员 */
    private Integer role;
    /** 头像相对路径（API.md v1.8），null=未设置 */
    private String avatar;
    /** 个性签名（API.md v1.8），null=未设置 */
    private String signature;
    /** 注册时间（UTC） */
    private LocalDateTime createdAt;

    public static LoginVO of(String token, User user) {
        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .avatar(user.getAvatar())
                .signature(user.getSignature())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
