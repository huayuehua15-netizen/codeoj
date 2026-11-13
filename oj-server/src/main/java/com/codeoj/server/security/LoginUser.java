package com.codeoj.server.security;

import java.io.Serializable;

/**
 * 安全上下文主体：由 JwtAuthFilter 解析令牌后注入（论文 5.2/5.3 @AuthenticationPrincipal）。
 */
public record LoginUser(Long userId, Integer role, String jti) implements Serializable {

    public boolean isAdmin() {
        return role != null && role == 1;
    }
}
