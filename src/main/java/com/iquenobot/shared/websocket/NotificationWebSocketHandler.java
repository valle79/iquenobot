package com.iquenobot.shared.websocket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@Slf4j
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<UUID>> conversationSubscribers = new ConcurrentHashMap<>();

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

        log.debug("WebSocket connected: user={} tenant={} sessions={}", userId, tenantId, userSessions.get(userUuid).size());
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
            removeUserFromAllConversations(userUuid);
        }
        log.debug("WebSocket disconnected: user={} status={}", userId, status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String userId = (String) session.getAttributes().get("userId");
        try {
            Map<String, Object> msg = objectMapper.readValue(message.getPayload(),
                    new TypeReference<Map<String, Object>>() {});
            String type = (String) msg.get("type");

            if ("conversation:join".equals(type)) {
                handleConversationJoin(session, userId, msg);
            } else if ("conversation:leave".equals(type)) {
                handleConversationLeave(session, userId, msg);
            } else {
                log.debug("WebSocket message from user={}: {}", userId, type);
            }
        } catch (Exception e) {
            log.debug("Malformed WebSocket message from user={}: {}", userId, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void handleConversationJoin(WebSocketSession session, String userId, Map<String, Object> msg) {
        Map<String, Object> payload = (Map<String, Object>) msg.get("payload");
        if (payload == null) return;

        String conversationId = (String) payload.get("conversationId");
        if (conversationId == null || conversationId.isBlank()) return;

        UUID userUuid = UUID.fromString(userId);
        Set<UUID> subscribers = conversationSubscribers.computeIfAbsent(
                conversationId, k -> new CopyOnWriteArraySet<>());
        subscribers.add(userUuid);

        log.debug("User {} joined conversation {} ({} subscribers)",
                userId, conversationId, subscribers.size());
    }

    @SuppressWarnings("unchecked")
    private void handleConversationLeave(WebSocketSession session, String userId, Map<String, Object> msg) {
        Map<String, Object> payload = (Map<String, Object>) msg.get("payload");
        if (payload == null) return;

        String conversationId = (String) payload.get("conversationId");
        if (conversationId == null || conversationId.isBlank()) return;

        UUID userUuid = UUID.fromString(userId);
        Set<UUID> subscribers = conversationSubscribers.get(conversationId);
        if (subscribers != null) {
            subscribers.remove(userUuid);
            if (subscribers.isEmpty()) {
                conversationSubscribers.remove(conversationId);
            }
        }

        log.debug("User {} left conversation {}", userId, conversationId);
    }

    private void removeUserFromAllConversations(UUID userId) {
        conversationSubscribers.forEach((conversationId, subscribers) -> {
            subscribers.remove(userId);
            if (subscribers.isEmpty()) {
                conversationSubscribers.remove(conversationId);
            }
        });
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("WebSocket transport error: user={} error={}",
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
                    log.debug("WebSocket message sent to user={} sessions={}", userId, sessions.size());
                } catch (IOException e) {
                    log.warn("Error sending WebSocket message to user={}: {}", userId, e.getMessage());
                }
            }
        }
    }

    public void sendToConversation(String conversationId, Object payload) {
        Set<UUID> subscribers = conversationSubscribers.get(conversationId);
        if (subscribers == null || subscribers.isEmpty()) return;

        for (UUID userId : subscribers) {
            sendToUser(userId, payload);
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
