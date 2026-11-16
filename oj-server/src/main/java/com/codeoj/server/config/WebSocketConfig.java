package com.codeoj.server.config;

import com.codeoj.server.ws.JudgeWebSocketInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 判题推送：浏览器在 STOMP CONNECT 原生头中发送 Authorization。
 * SockJS 的 /ws/info、/ws/xhr 等 HTTP 端点仍由 SecurityConfig 放行，
 * 单一入站拦截器完成令牌和账号校验，并将 LoginUser 绑定为会话 Principal。
 * Principal.getName() 返回 userId 字符串，与 convertAndSendToUser 的目标一致。
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JudgeWebSocketInterceptor interceptor;

    public WebSocketConfig(JudgeWebSocketInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(interceptor);
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
}
