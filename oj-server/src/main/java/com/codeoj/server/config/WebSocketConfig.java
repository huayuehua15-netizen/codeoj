package com.codeoj.server.config;

import com.codeoj.server.security.JwtUtil;
import com.codeoj.server.security.LoginUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 实时推送配置（论文 5.5）：STOMP 端点 /ws，判题状态经 /user/queue/judge 定向推送。
 *
 * <p><b>鉴权在 STOMP CONNECT 帧上完成</b>（不是握手阶段）：
 * SockJS 的 info/xhr 端点是普通 HTTP 请求，握手期拿不到 Authorization 头，
 * 因此 {@code /ws/**} 在 SecurityConfig 里仍是 permitAll，真正的身份校验放在
 * 入站通道拦截器里——校验失败则不注入 Principal，后续 {@code /user/**} 订阅
 * 因解析不到用户而被框架拒绝（fail-closed）。
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    private final JwtUtil jwtUtil;

    public WebSocketConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry r) {
        r.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry r) {
        r.enableSimpleBroker("/topic", "/queue");
        r.setUserDestinationPrefix("/user");
    }

    /**
     * CONNECT 帧鉴权：解析 Authorization（STOMP 原生头）并绑定 Principal。
     * 缺省 token 时放行 CONNECT，但会话无 Principal，
     * {@code convertAndSendToUser} 不会投递给它（不会串发到别人）。
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String raw = accessor.getFirstNativeHeader("Authorization");
                    if (raw == null) {
                        raw = accessor.getFirstNativeHeader("authorization");
                    }
                    if (raw != null && raw.startsWith("Bearer ")) {
                        jwtUtil.tryParse(raw.substring(7)).ifPresent(claims -> {
                            LoginUser user = new LoginUser(
                                    Long.valueOf(claims.getSubject()),
                                    (Integer) claims.get("role"),
                                    claims.getId());
                            accessor.setUser(user);
                            log.debug("stomp connect authenticated, userId={}", user.userId());
                        });
                    }
                }
                return message;
            }
        });
    }
}
