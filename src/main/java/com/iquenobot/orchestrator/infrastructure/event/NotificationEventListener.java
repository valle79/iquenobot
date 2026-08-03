package com.iquenobot.orchestrator.infrastructure.event;

import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.conversation.domain.entity.ConversationMessage;
import com.iquenobot.conversation.domain.entity.MessageAttachment;
import com.iquenobot.conversation.domain.repository.ConversationMessageRepository;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.conversation.domain.repository.MessageAttachmentRepository;
import com.iquenobot.contact.domain.repository.ContactRepository;
import com.iquenobot.notification.application.NotificationService;
import com.iquenobot.notification.domain.dto.CreateNotificationRequestDto;
import com.iquenobot.orchestrator.interfaces.event.*;
import com.iquenobot.shared.domain.util.TenantContext;
import com.iquenobot.shared.enums.AttachmentType;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.NotificationChannel;
import com.iquenobot.shared.enums.NotificationPriority;
import com.iquenobot.shared.enums.NotificationType;
import com.iquenobot.shared.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final NotificationWebSocketHandler webSocketHandler;
    private final ConversationMessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ContactRepository contactRepository;
    private final MessageAttachmentRepository attachmentRepository;

    @Async
    @EventListener
    public void handleMessageReceived(MessageReceivedEvent event) {
        try {
            UUID tenantId = UUID.fromString(event.getTenantId());
            UUID conversationId = UUID.fromString(event.getConversationId());

            Optional<ConversationMessage> persisted = Optional.empty();
            if (event.getMessageId() != null) {
                // Fetch con attachments eager para evitar LazyInitializationException
                // fuera de la transacción (el listener es @Async).
                persisted = messageRepository.findByChannelMessageIdAndTenantIdWithAttachments(
                        event.getMessageId(), tenantId);
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
                payload.put("attachments", buildAttachmentsPayload(msg.getAttachments()));
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
                payload.put("attachments", List.of());
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

                // Enriquecer la notificación con datos del contacto y el último mensaje del cliente
                String contactName = null;
                String avatarUrl = null;
                ChannelType channel = null;

                if (event.getContactId() != null) {
                    var contactOpt = contactRepository.findByIdAndTenantIdAndDeletedFalse(
                            UUID.fromString(event.getContactId()), tenantId);
                    if (contactOpt.isPresent()) {
                        contactName = contactOpt.get().getFullName();
                        avatarUrl = contactOpt.get().getAvatarUrl();
                    }
                }

                var conversationOpt = conversationRepository.findByIdAndTenantIdAndDeletedFalse(conversationId, tenantId);
                if (conversationOpt.isPresent()) {
                    channel = conversationOpt.get().getChannel();
                }

                String lastCustomerMessage = null;
                String attachmentSummary = null;
                try {
                    var latest = messageRepository.findLatestCustomerMessages(
                                    conversationId, tenantId, PageRequest.of(0, 1))
                            .stream()
                            .findFirst()
                            .orElse(null);
                    if (latest != null) {
                        lastCustomerMessage = truncate(latest.getContent(), 140);
                        attachmentSummary = summarizeAttachments(latest.getId());
                    }
                } catch (Exception ex) {
                    log.warn("Error cargando el último mensaje del cliente para la notificación: {}", ex.getMessage());
                }

                String title = contactName != null && !contactName.isBlank()
                        ? "Nueva conversación de " + contactName
                        : "Conversación Asignada";

                StringBuilder message = new StringBuilder();
                if (lastCustomerMessage != null && !lastCustomerMessage.isBlank()) {
                    message.append("«").append(lastCustomerMessage).append("»");
                } else if (attachmentSummary != null) {
                    message.append(attachmentSummary);
                } else {
                    message.append("Se te ha asignado una nueva conversación");
                }
                if (channel != null) {
                    message.append(" · vía ").append(channelLabel(channel));
                }

                CreateNotificationRequestDto notification = CreateNotificationRequestDto.builder()
                        .userId(agentId)
                        .type(NotificationType.CONVERSATION_ASSIGNED)
                        .channel(NotificationChannel.IN_APP)
                        .priority(NotificationPriority.HIGH)
                        .title(title)
                        .message(message.toString())
                        .actionUrl("/conversations/" + event.getConversationId())
                        .actionLabel("Atender Ahora")
                        .icon("assignment")
                        .imageUrl(avatarUrl)
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

            // Adjuntar los archivos del mensaje (imagen, audio, etc.)
            if (event.getMessageId() != null) {
                try {
                    messageRepository.findByIdAndTenantIdWithAttachments(
                                    UUID.fromString(event.getMessageId()), tenantId)
                            .ifPresent(m -> payload.put("attachments",
                                    buildAttachmentsPayload(m.getAttachments())));
                } catch (Exception ex) {
                    log.warn("Error cargando attachments del mensaje enviado: {}", ex.getMessage());
                }
            }
            if (!payload.containsKey("attachments")) {
                payload.put("attachments", List.of());
            }

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

    private List<Map<String, Object>> buildAttachmentsPayload(Set<MessageAttachment> attachments) {        List<Map<String, Object>> result = new ArrayList<>();
        if (attachments == null) {
            return result;
        }
        for (MessageAttachment att : attachments) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", att.getId() != null ? att.getId().toString() : null);
            map.put("type", att.getType() != null ? att.getType().name() : null);
            map.put("fileName", att.getFileName());
            map.put("fileUrl", att.getFileUrl());
            map.put("fileSize", att.getFileSize());
            map.put("formattedFileSize", att.getFormattedFileSize());
            map.put("mimeType", att.getMimeType());
            map.put("thumbnailUrl", att.getThumbnailUrl());
            map.put("durationSeconds", att.getDurationSeconds());
            map.put("width", att.getWidth());
            map.put("height", att.getHeight());
            map.put("caption", att.getCaption());
            result.add(map);
        }
        return result;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }

    private String summarizeAttachments(UUID messageId) {
        List<MessageAttachment> attachments = attachmentRepository.findByMessageId(messageId);
        if (attachments.isEmpty()) {
            return null;
        }
        long imageCount = attachments.stream()
                .filter(a -> a.getType() == AttachmentType.IMAGE)
                .count();
        if (imageCount > 0) {
            return imageCount == 1 ? "Envió una imagen" : "Envió " + imageCount + " imágenes";
        }
        return "Envió un archivo adjunto";
    }

    private String channelLabel(ChannelType channel) {
        return switch (channel) {
            case WHATSAPP -> "WhatsApp";
            case TELEGRAM -> "Telegram";
            case MESSENGER -> "Messenger";
            case INSTAGRAM -> "Instagram";
            case EMAIL -> "Email";
            case WEBCHAT -> "Web";
            case SMS -> "SMS";
            case TWITTER -> "Twitter";
            case API -> "API";
        };
    }
}
