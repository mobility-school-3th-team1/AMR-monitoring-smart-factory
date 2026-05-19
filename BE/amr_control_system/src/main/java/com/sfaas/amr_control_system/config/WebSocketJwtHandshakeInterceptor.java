package com.sfaas.amr_control_system.config;

import com.sfaas.amr_control_system.security.JwtUtil;
import com.sfaas.amr_control_system.websocket.WebSocketConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketJwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }

        String token = servletRequest.getServletRequest().getParameter(WebSocketConstants.QUERY_PARAM_TOKEN);
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            String username = jwtUtil.extractUsername(token);
            if (username == null || username.isBlank()) {
                return false;
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (!jwtUtil.isAccessTokenValid(token, userDetails)) {
                return false;
            }

            attributes.put("username", username);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // no-op
    }
}
