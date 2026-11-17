package com.codeoj.server.ws;

import com.codeoj.common.entity.User;
import com.codeoj.server.mapper.UserMapper;
import com.codeoj.server.security.JwtUtil;
import com.codeoj.server.security.LoginUser;
import com.codeoj.server.security.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** SockJS authenticates the STOMP CONNECT frame because browsers cannot add HTTP headers. */
@Component
public class JudgeWebSocketInterceptor implements ChannelInterceptor {
    private static final Logger log = LoggerFactory.getLogger(JudgeWebSocketInterceptor.class);
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklistService;
    private final UserMapper userMapper;

    public JudgeWebSocketInterceptor(JwtUtil jwtUtil, TokenBlacklistService blacklistService,
                                     UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.blacklistService = blacklistService;
        this.userMapper = userMapper;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) return message;
        if (accessor.getCommand() == StompCommand.CONNECT || accessor.getCommand() == StompCommand.STOMP) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header == null) header = accessor.getFirstNativeHeader("authorization");
            if (header == null || !header.startsWith("Bearer ")) throw denied();
            Claims claims = jwtUtil.tryParse(header.substring(7)).orElseThrow(JudgeWebSocketInterceptor::denied);
            if (claims.getId() == null || claims.getId().isBlank() || blacklistService.contains(claims.getId())) throw denied();
            Long userId;
            try {
                userId = Long.valueOf(claims.getSubject());
            } catch (RuntimeException e) {
                throw denied();
            }
            User user = userMapper.selectById(userId);
            if (user == null || Integer.valueOf(1).equals(user.getStatus())) throw denied();
            // This name must match convertAndSendToUser(String.valueOf(userId), ...).
            // Keep B's Principal contract; current DB role takes precedence over a stale JWT claim.
            accessor.setUser(new LoginUser(user.getId(), user.getRole(), claims.getId()));
            log.debug("stomp connect authenticated, userId={}", userId);
        } else if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            if (!(accessor.getUser() instanceof LoginUser) || !"/user/queue/judge".equals(accessor.getDestination())) throw denied();
        } else if (accessor.getCommand() == StompCommand.SEND) {
            // Judge notifications are receive-only; clients cannot publish fake events.
            throw denied();
        }
        return message;
    }

    private static AccessDeniedException denied() {
        return new AccessDeniedException("WebSocket authentication or destination denied");
    }
}
