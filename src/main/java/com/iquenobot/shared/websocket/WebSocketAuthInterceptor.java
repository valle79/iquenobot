package com.iquenobot.shared.websocket;

import com.iquenobot.security.application.JwtService;
import com.iquenobot.shared.domain.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String query = request.getURI().getQuery();
        if (query == null || query.isBlank()) {
            log.warn("WebSocket handshake without token");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        String token = null;
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2 && "token".equals(pair[0])) {
                token = pair[1];
                break;
            }
        }

        if (token == null || token.isBlank()) {
            log.warn("WebSocket handshake without token parameter");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        if (!jwtService.isTokenValid(token)) {
            log.warn("WebSocket handshake with invalid token");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        attributes.put("userId", jwtService.extractUserId(token).toString());
        attributes.put("tenantId", jwtService.extractTenantId(token).toString());

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
