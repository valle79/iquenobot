package com.iquenobot.shared.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationHandler;
    private final WebSocketAuthInterceptor authInterceptor;

    public WebSocketConfig(NotificationWebSocketHandler notificationHandler,
                           WebSocketAuthInterceptor authInterceptor) {
        this.notificationHandler = notificationHandler;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationHandler, "/ws/notifications")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins("*");
    }
}
