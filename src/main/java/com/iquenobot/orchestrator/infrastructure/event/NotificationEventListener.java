package com.iquenobot.orchestrator.infrastructure.event;

import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.conversation.domain.entity.ConversationMessage;
import com.iquenobot.conversation.domain.repository.ConversationMessageRepository;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.notification.application.NotificationService;
import com.iquenobot.notification.domain.dto.CreateNotificationRequestDto;
import com.iquenobot.orchestrator.interfaces.event.*;
import com.iquenobot.shared.domain.util.TenantContext;
import com.iquenobot.shared.enums.NotificationChannel;
import com.iquenobot.shared.enums.NotificationPriority;
import com.iquenobot.shared.enums.NotificationType;
import com.iquenobot.shared.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final NotificationWebSocketHandler webSocketHandler;
    private final ConversationMessageRepository messageRepository;
    private final ConversationRepository conversationRepository;

    @Async
    @EventListener
    public void handleMessageReceived(MessageReceivedEvent event) {
        try {
            UUID tenantId = UUID.fromString(event.getTenantId());
            UUID conversationId = UUID.fromString(event.getConversationId());

            Optional<ConversationMessage> persisted = Optional.empty();
            if (event.getMessageId() != null) {
                persisted = messageRepository.findByChannelMessageIdAndTenantId(event.getMessageId(), tenantId);
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            if (persisted.isPresent()) {
                ConversationMessage msg = persisted.get();
                payload.put("id", msg.getId().toString());
                payload.put("conversationId", msg.getConversation().getId().toString());
                payload.put("content", msg.getContent());
                payload.put("sentAt", msg.getSentAt() != null ? msg.getSentAt().toString() : null);
                payload.put("direction", msg.getDirection() != null ? msg.getDirection().name() : "INBOUND");
                payload.put("type", msg.getType() != null ? msg.getType().name() : "TEXT");
                payload.put("status", msg.getStatus() != null ? msg.getStatus().name() : "SENT");
                payload.put("fromBot", msg.isFromBot());
                payload.put("senderName", msg.getSenderName());
            } else {
                payload.put("id", UUID.randomUUID().toString());
                payload.put("conversationId", event.getConversationId());
                payload.put("content", event.getMessage() != null ? event.getMessage().getContent() : "");
                payload.put("sentAt", LocalDateTime.now(ZoneOffset.UTC).toString());
                payload.put("direction", "INBOUND");
                payload.put("type", "TEXT");
                payload.put("status", "SENT");
                payload.put("fromBot", false);
                payload.put("senderName", event.getMessage() != null ? event.getMessage().getSourceName() : "");
            }

            webSocketHandler.sendToTenant(tenantId, Map.of("type", "message:new", "payload", payload));

            updateConversationLastMessage(conversationId, payload);

        } catch (Exception e) {
            log.error("Error pushing message:new via WebSocket", e);
        }
    }

    @Async
    @EventListener
    public void handleConversationCreated(ConversationCreatedEvent event) {
        try {
            UUID tenantId = UUID.fromString(event.getTenantId());
            UUID conversationId = UUID.fromString(event.getConversationId());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("id", conversationId.toString());

            // Usar datos del evento en lugar de consultar DB (evita race condition con @Transactional)
            String contactName = event.getContactName();
            if (contactName != null && !contactName.isBlank()) {
                Map<String, Object> contactData = new LinkedHashMap<>();
                contactData.put("fullName", contactName);
                contactData.put("displayName", contactName);
                payload.put("contact", contactData);
            }

            webSocketHandler.sendToTenant(tenantId, Map.of("type", "conversation:new", "payload", payload));

        } catch (Exception e) {
            log.error("Error pushing conversation:new via WebSocket", e);
        }
    }

    @Async
    @EventListener
    public void handleAgentAssigned(AgentAssignedEvent event) {
        try {
            UUID tenantId = UUID.fromString(event.getTenantId());
            UUID conversationId = UUID.fromString(event.getConversationId());

            webSocketHandler.sendToTenant(tenantId, Map.of(
                    "type", "conversation:updated",
                    "payload", Map.of(
                            "id", event.getConversationId(),
                            "status", "IN_PROGRESS"
                    )
            ));

            if (event.getAgentId() != null) {
                UUID agentId = UUID.fromString(event.getAgentId());

                webSocketHandler.sendToUser(agentId, Map.of(
                        "type", "conversation:assigned",
                        "payload", Map.of(
                                "conversationId", event.getConversationId(),
                                "agentId", event.getAgentId(),
                                "contactId", event.getContactId()
                        )
                ));

                TenantContext.setTenantId(event.getTenantId());

                CreateNotificationRequestDto notification = CreateNotificationRequestDto.builder()
                        .userId(agentId)
                        .type(NotificationType.CONVERSATION_ASSIGNED)
                        .channel(NotificationChannel.IN_APP)
                        .priority(NotificationPriority.HIGH)
                        .title("Conversación Asignada")
                        .message("Se te ha asignado una nueva conversación")
                        .actionUrl("/conversations/" + event.getConversationId())
                        .actionLabel("Atender Ahora")
                        .icon("assignment")
                        .relatedEntityType("Conversation")
                        .relatedEntityId(event.getConversationId())
                        .build();

                notificationService.create(notification);
            }

        } catch (Exception e) {
            log.error("Error handling agent assigned event", e);
        }
    }

    @Async
    @EventListener
    public void handleBotAnswered(BotAnsweredEvent event) {
        try {
            UUID tenantId = UUID.fromString(event.getTenantId());
            UUID conversationId = UUID.fromString(event.getConversationId());

            Optional<ConversationMessage> latestBotMsg = messageRepository
                    .findByConversationIdOrderBySentAtAsc(conversationId)
                    .stream()
                    .filter(m -> m.isFromBot())
                    .reduce((a, b) -> b);

            Map<String, Object> payload = new LinkedHashMap<>();
            if (latestBotMsg.isPresent()) {
                ConversationMessage msg = latestBotMsg.get();
                payload.put("id", msg.getId().toString());
                payload.put("conversationId", msg.getConversation().getId().toString());
                payload.put("content", msg.getContent());
                payload.put("sentAt", msg.getSentAt() != null ? msg.getSentAt().toString() : null);
                payload.put("direction", "OUTBOUND");
                payload.put("type", msg.getType() != null ? msg.getType().name() : "TEXT");
                payload.put("status", msg.getStatus() != null ? msg.getStatus().name() : "SENT");
                payload.put("fromBot", true);
                payload.put("botIntent", msg.getBotIntent());
            } else {
                payload.put("id", UUID.randomUUID().toString());
                payload.put("conversationId", event.getConversationId());
                payload.put("content", event.getResponsePreview());
                payload.put("sentAt", LocalDateTime.now(ZoneOffset.UTC).toString());
                payload.put("direction", "OUTBOUND");
                payload.put("type", "TEXT");
                payload.put("status", "SENT");
                payload.put("fromBot", true);
            }

            webSocketHandler.sendToTenant(tenantId, Map.of("type", "message:new", "payload", payload));

            updateConversationLastMessage(conversationId, payload);

        } catch (Exception e) {
            log.error("Error pushing bot answer via WebSocket", e);
        }
    }

    @Async
    @EventListener
    public void handleConversationClosed(ConversationClosedEvent event) {
        try {
            UUID tenantId = UUID.fromString(event.getTenantId());

            webSocketHandler.sendToTenant(tenantId, Map.of(
                    "type", "conversation:updated",
                    "payload", Map.of(
                            "id", event.getConversationId(),
                            "status", "CLOSED"
                    )
            ));
        } catch (Exception e) {
            log.error("Error pushing conversation:closed via WebSocket", e);
        }
    }

    @Async
    @EventListener
    public void handleMessageSent(MessageSentEvent event) {
        try {
            UUID tenantId = UUID.fromString(event.getTenantId());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("id", event.getMessageId());
            payload.put("conversationId", event.getConversationId());
            payload.put("content", event.getContent());
            payload.put("sentAt", LocalDateTime.now(ZoneOffset.UTC).toString());
            payload.put("direction", event.getDirection());
            payload.put("type", event.getType());
            payload.put("status", "SENT");
            payload.put("fromBot", false);
            payload.put("senderName", event.getSenderName());

            webSocketHandler.sendToTenant(tenantId, Map.of("type", "message:new", "payload", payload));

            updateConversationLastMessage(UUID.fromString(event.getConversationId()), payload);

        } catch (Exception e) {
            log.error("Error pushing message:sent via WebSocket", e);
        }
    }

    @Async
    @EventListener
    public void handleActionExecuted(ActionExecutedEvent event) {
        log.debug("Action executed event processed: action={}", event.getActionType());
    }

    private void updateConversationLastMessage(UUID conversationId, Map<String, Object> messagePayload) {
        try {
            Map<String, Object> convUpdate = new LinkedHashMap<>();
            convUpdate.put("id", conversationId.toString());
            convUpdate.put("lastMessage", messagePayload);
            convUpdate.put("lastMessageAt", messagePayload.get("sentAt"));

            UUID tenantId = messagePayload.containsKey("conversationId") ? null : null;
            if (messagePayload.containsKey("conversationId")) {
                Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
                if (convOpt.isPresent()) {
                    Conversation conv = convOpt.get();
                    convUpdate.put("messageCount", conv.getMessageCount());
                    tenantId = conv.getTenantId();
                }
            }

            if (tenantId == null) {
                Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
                if (convOpt.isPresent()) {
                    tenantId = convOpt.get().getTenantId();
                }
            }

            if (tenantId != null) {
                webSocketHandler.sendToTenant(tenantId, Map.of("type", "conversation:updated", "payload", convUpdate));
            }
        } catch (Exception e) {
            log.warn("Error updating conversation via WebSocket: {}", e.getMessage());
        }
    }
}
