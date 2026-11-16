package com.codeoj.server.security;

import java.io.Serializable;
import java.security.Principal;

/**
 * 安全上下文主体：由 JwtAuthFilter 解析令牌后注入（论文 5.2/5.3 @AuthenticationPrincipal）。
 *
 * <p>实现 {@link Principal} 是为了 WebSocket 推送（论文 4.5/5.5）：
 * {@code convertAndSendToUser(userId, ...)} 依赖 STOMP 会话绑定的 Principal，
 * 且按 {@link #getName()} 的返回值匹配目标用户——因此 {@code getName()} 必须返回
 * **userId 的字符串形式**（与 {@code PushEventPublisher} 传参一致），不能是 username。
 */
public record LoginUser(Long userId, Integer role, String jti)
        implements Serializable, Principal {

    public boolean isAdmin() {
        return role != null && role == 1;
    }

    /** convertAndSendToUser 按此名匹配会话，必须是 userId 的字符串形式 */
    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
