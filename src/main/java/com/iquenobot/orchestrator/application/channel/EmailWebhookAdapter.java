package com.iquenobot.orchestrator.application.channel;

import com.iquenobot.orchestrator.application.ConversationOrchestrator;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingResult;
import com.iquenobot.orchestrator.interfaces.dto.EmailWebhookDto;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailWebhookAdapter {

    private final ConversationOrchestrator orchestrator;

    public ProcessingResult processWebhook(String instanceId, EmailWebhookDto payload) {
        String fullContent = buildEmailContent(payload);

        IncomingMessage message = IncomingMessage.builder()
                .channelMessageId(payload.getMessageId())
                .channel(ChannelType.EMAIL)
                .sourceIdentifier(payload.getFrom())
                .sourceName(payload.getFromName())
                .type(MessageType.TEXT)
                .content(fullContent)
                .instanceId(instanceId)
                .timestamp(payload.getReceivedAt())
                .build();

        return orchestrator.processMessage(message);
    }

    private String buildEmailContent(EmailWebhookDto payload) {
        StringBuilder sb = new StringBuilder();
        if (payload.getSubject() != null && !payload.getSubject().isBlank()) {
            sb.append("Subject: ").append(payload.getSubject()).append("\n\n");
        }
        if (payload.getBodyText() != null) {
            sb.append(payload.getBodyText());
        } else if (payload.getBodyHtml() != null) {
            sb.append(payload.getBodyHtml());
        }
        return sb.toString();
    }
}
