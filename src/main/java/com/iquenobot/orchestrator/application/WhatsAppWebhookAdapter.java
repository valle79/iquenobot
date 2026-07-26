package com.iquenobot.orchestrator.application;

import com.iquenobot.ai.domain.dto.WhatsAppWebhookDto;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingResult;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookAdapter {

    private final ConversationOrchestrator orchestrator;

    public ProcessingResult processWebhook(String instanceId, WhatsAppWebhookDto payload) {
        IncomingMessage message = convertToIncomingMessage(instanceId, payload);
        return orchestrator.processMessage(message);
    }

    private IncomingMessage convertToIncomingMessage(String instanceId, WhatsAppWebhookDto payload) {
        MessageType type = resolveMessageType(payload.getType());

        return IncomingMessage.builder()
                .channelMessageId(payload.getMessageId())
                .channel(ChannelType.WHATSAPP)
                .sourceIdentifier(payload.getFrom())
                .sourceName(payload.getData() != null
                        ? (String) payload.getData().get("pushName")
                        : null)
                .type(type)
                .content(payload.getText())
                .mediaUrl(payload.getMediaUrl())
                .caption(payload.getCaption())
                .filename(payload.getFilename())
                .mimeType(payload.getMimeType())
                .timestamp(payload.getTimestamp())
                .instanceId(instanceId)
                .metadata(payload.getData())
                .build();
    }

    private MessageType resolveMessageType(String type) {
        if (type == null) return MessageType.TEXT;
        return switch (type.toUpperCase()) {
            case "IMAGE" -> MessageType.IMAGE;
            case "VIDEO" -> MessageType.VIDEO;
            case "AUDIO" -> MessageType.AUDIO;
            case "DOCUMENT" -> MessageType.DOCUMENT;
            case "LOCATION" -> MessageType.LOCATION;
            case "CONTACT" -> MessageType.CONTACT;
            case "STICKER" -> MessageType.STICKER;
            default -> MessageType.TEXT;
        };
    }
}
