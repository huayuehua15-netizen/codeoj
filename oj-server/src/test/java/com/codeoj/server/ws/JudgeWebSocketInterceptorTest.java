package com.codeoj.server.ws;

import com.codeoj.common.entity.User;
import com.codeoj.server.mapper.UserMapper;
import com.codeoj.server.security.JwtUtil;
import com.codeoj.server.security.LoginUser;
import com.codeoj.server.security.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JudgeWebSocketInterceptorTest {
    private final JwtUtil jwt = mock(JwtUtil.class);
    private final TokenBlacklistService blacklist = mock(TokenBlacklistService.class);
    private final UserMapper users = mock(UserMapper.class);
    private final JudgeWebSocketInterceptor interceptor = new JudgeWebSocketInterceptor(jwt, blacklist, users);

    @BeforeEach void setup() {
        Claims claims = Jwts.claims().subject("42").id("valid-jti").add("role", 1).build();
        when(jwt.tryParse("valid-token")).thenReturn(Optional.of(claims));
        User user = new User(); user.setId(42L); user.setStatus(0); user.setRole(0);
        when(users.selectById(42L)).thenReturn(user);
    }

    @Test void connectBindsVerifiedNumericUserId() {
        StompHeaderAccessor h = StompHeaderAccessor.create(StompCommand.CONNECT);
        h.addNativeHeader("Authorization", "Bearer valid-token");
        interceptor.preSend(message(h), null);
        assertEquals("42", h.getUser().getName());
        LoginUser principal = assertInstanceOf(LoginUser.class, h.getUser());
        assertEquals(0, principal.role(), "DB role must override the stale admin claim");
        assertEquals("valid-jti", principal.jti());
    }

    @Test void lowercaseAuthorizationHeaderAndStomp12CommandAreSupported() {
        StompHeaderAccessor h = StompHeaderAccessor.create(StompCommand.STOMP);
        h.addNativeHeader("authorization", "Bearer valid-token");
        interceptor.preSend(message(h), null);
        assertEquals("42", h.getUser().getName());
        assertInstanceOf(LoginUser.class, h.getUser());
    }

    @Test void missingOrInvalidCredentialsAreRejected() {
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(
                StompHeaderAccessor.create(StompCommand.CONNECT)), null));
        StompHeaderAccessor h = StompHeaderAccessor.create(StompCommand.CONNECT);
        h.addNativeHeader("Authorization", "Bearer expired");
        when(jwt.tryParse("expired")).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(h), null));
    }

    @Test void loggedOutAndBannedUsersCannotConnect() {
        when(blacklist.contains("valid-jti")).thenReturn(true);
        assertThrows(AccessDeniedException.class, () -> connect());
        when(blacklist.contains("valid-jti")).thenReturn(false);
        User user = new User(); user.setId(42L); user.setStatus(1);
        when(users.selectById(42L)).thenReturn(user);
        assertThrows(AccessDeniedException.class, () -> connect());
        when(users.selectById(42L)).thenReturn(null);
        assertThrows(AccessDeniedException.class, () -> connect());
    }

    @Test void missingJtiAndMalformedSubjectAreRejected() {
        for (Claims claims : new Claims[]{Jwts.claims().subject("42").build(),
                Jwts.claims().subject("not-a-number").id("valid-jti").build()}) {
            when(jwt.tryParse("valid-token")).thenReturn(Optional.of(claims));
            assertThrows(AccessDeniedException.class, () -> connect());
        }
    }

    @Test void onlyAuthenticatedUserDestinationIsAllowed() {
        StompHeaderAccessor valid = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        valid.setUser(new LoginUser(42L, 0, "valid-jti")); valid.setDestination("/user/queue/judge");
        assertNotNull(interceptor.preSend(message(valid), null));
        for (String destination : new String[]{"/queue/judge", "/user/99/queue/judge", "/topic/judge"}) {
            StompHeaderAccessor h = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
            h.setUser(new LoginUser(42L, 0, "valid-jti")); h.setDestination(destination);
            assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(h), null));
        }
        StompHeaderAccessor anonymous = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        anonymous.setDestination("/user/queue/judge");
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(anonymous), null));
    }

    @Test void clientsCannotPublishFakeJudgments() {
        StompHeaderAccessor h = StompHeaderAccessor.create(StompCommand.SEND);
        h.setUser(() -> "42"); h.setDestination("/queue/judge");
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(h), null));
    }

    private void connect() {
        StompHeaderAccessor h = StompHeaderAccessor.create(StompCommand.CONNECT);
        h.addNativeHeader("Authorization", "Bearer valid-token");
        interceptor.preSend(message(h), null);
    }

    private Message<byte[]> message(StompHeaderAccessor h) {
        h.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], h.getMessageHeaders());
    }
}
