package com.iquenobot.shared.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iquenobot.shared.domain.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    public NotificationWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = (String) session.getAttributes().get("userId");
        String tenantId = (String) session.getAttributes().get("tenantId");

        if (userId == null || tenantId == null) {
            try {
                session.close(CloseStatus.BAD_DATA);
            } catch (IOException e) {
                log.warn("Error closing websocket session: {}", e.getMessage());
            }
            return;
        }

        UUID userUuid = UUID.fromString(userId);
        userSessions.computeIfAbsent(userUuid, k -> new CopyOnWriteArrayList<>()).add(session);

        log.info("WebSocket connected: user={} tenant={} sessions={}", userId, tenantId, userSessions.get(userUuid).size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            UUID userUuid = UUID.fromString(userId);
            CopyOnWriteArrayList<WebSocketSession> sessions = userSessions.get(userUuid);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userUuid);
                }
            }
        }
        log.info("WebSocket disconnected: user={} status={}", userId, status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String userId = (String) session.getAttributes().get("userId");
        log.debug("WebSocket message from user={}: {}", userId, message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket transport error: user={} error={}",
                session.getAttributes().get("userId"), exception.getMessage());
    }

    public void sendToUser(UUID userId, Object payload) {
        CopyOnWriteArrayList<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) return;

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Error serializing WebSocket payload for user={}", userId, e);
            return;
        }

        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    log.warn("Error sending WebSocket message to user={}: {}", userId, e.getMessage());
                }
            }
        }
    }

    public void sendToTenant(UUID tenantId, Object payload) {
        userSessions.forEach((userId, sessions) -> {
            sessions.stream()
                    .filter(s -> tenantId.toString().equals(s.getAttributes().get("tenantId")))
                    .findFirst()
                    .ifPresent(s -> sendToUser(userId, payload));
        });
    }

    public void broadcast(Object payload) {
        userSessions.keySet().forEach(userId -> sendToUser(userId, payload));
    }

    public int getActiveConnections() {
        return userSessions.values().stream().mapToInt(CopyOnWriteArrayList::size).sum();
    }
}
