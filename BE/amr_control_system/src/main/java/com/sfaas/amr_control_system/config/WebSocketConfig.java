package com.sfaas.amr_control_system.config;

import com.sfaas.amr_control_system.websocket.StreamWebSocketHandler;
import com.sfaas.amr_control_system.websocket.WebSocketConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final StreamWebSocketHandler streamWebSocketHandler;
    private final WebSocketJwtHandshakeInterceptor webSocketJwtHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(streamWebSocketHandler, WebSocketConstants.STREAM_PATH)
                .addInterceptors(webSocketJwtHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
