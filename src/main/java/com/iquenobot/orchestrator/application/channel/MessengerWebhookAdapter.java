package com.iquenobot.orchestrator.application.channel;

import com.iquenobot.orchestrator.application.ConversationOrchestrator;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingResult;
import com.iquenobot.orchestrator.interfaces.dto.MessengerWebhookDto;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessengerWebhookAdapter {

    private final ConversationOrchestrator orchestrator;

    public ProcessingResult processWebhook(String instanceId, MessengerWebhookDto payload) {
        if (payload.getEntry() == null || payload.getEntry().isEmpty()) {
            log.warn("Messenger webhook with no entries");
            return ProcessingResult.failure("NO_ENTRIES", "Webhook contains no entries", 0);
        }

        MessengerWebhookDto.Messaging firstMsg = payload.getEntry().get(0).getMessaging().get(0);

        IncomingMessage message = IncomingMessage.builder()
                .channelMessageId(firstMsg.getMessage() != null ? firstMsg.getMessage().getMid() : null)
                .channel(ChannelType.MESSENGER)
                .sourceIdentifier(firstMsg.getSender() != null ? firstMsg.getSender().getId() : null)
                .sourceName(null)
                .type(resolveMessageType(firstMsg))
                .content(firstMsg.getMessage() != null ? firstMsg.getMessage().getText() : null)
                .mediaUrl(extractMediaUrl(firstMsg))
                .instanceId(instanceId)
                .build();

        return orchestrator.processMessage(message);
    }

    private MessageType resolveMessageType(MessengerWebhookDto.Messaging msg) {
        if (msg.getMessage() == null) return MessageType.TEXT;
        if (msg.getMessage().getAttachments() != null && !msg.getMessage().getAttachments().isEmpty()) {
            String type = msg.getMessage().getAttachments().get(0).getType();
            if (type != null) {
                return switch (type) {
                    case "image" -> MessageType.IMAGE;
                    case "video" -> MessageType.VIDEO;
                    case "audio" -> MessageType.AUDIO;
                    case "file" -> MessageType.DOCUMENT;
                    case "location" -> MessageType.LOCATION;
                    default -> MessageType.TEXT;
                };
            }
        }
        return MessageType.TEXT;
    }

    private String extractMediaUrl(MessengerWebhookDto.Messaging msg) {
        if (msg.getMessage() == null || msg.getMessage().getAttachments() == null) return null;
        return msg.getMessage().getAttachments().stream()
                .findFirst()
                .map(a -> a.getPayload() != null ? a.getPayload().getUrl() : null)
                .orElse(null);
    }
}
