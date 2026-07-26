package com.iquenobot.orchestrator.application.channel;

import com.iquenobot.orchestrator.application.ConversationOrchestrator;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingResult;
import com.iquenobot.orchestrator.interfaces.dto.WebchatWebhookDto;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebchatWebhookAdapter {

    private final ConversationOrchestrator orchestrator;

    public ProcessingResult processWebhook(String instanceId, WebchatWebhookDto payload) {
        IncomingMessage message = IncomingMessage.builder()
                .channelMessageId(payload.getSessionId())
                .channel(ChannelType.WEBCHAT)
                .sourceIdentifier(payload.getSourceIdentifier() != null
                        ? payload.getSourceIdentifier() : payload.getVisitorId())
                .sourceName(payload.getSourceName())
                .type(resolveType(payload.getType()))
                .content(payload.getMessage())
                .instanceId(instanceId)
                .timestamp(payload.getTimestamp())
                .metadata(payload.getMetadata())
                .build();

        return orchestrator.processMessage(message);
    }

    private MessageType resolveType(String type) {
        if (type == null) return MessageType.TEXT;
        return switch (type.toUpperCase()) {
            case "IMAGE" -> MessageType.IMAGE;
            case "VIDEO" -> MessageType.VIDEO;
            case "AUDIO" -> MessageType.AUDIO;
            case "FILE" -> MessageType.DOCUMENT;
            default -> MessageType.TEXT;
        };
    }
}
