package com.iquenobot.orchestrator.application.channel;

import com.iquenobot.orchestrator.application.ConversationOrchestrator;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingResult;
import com.iquenobot.orchestrator.interfaces.dto.SmsWebhookDto;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmsWebhookAdapter {

    private final ConversationOrchestrator orchestrator;

    public ProcessingResult processWebhook(String instanceId, SmsWebhookDto payload) {
        IncomingMessage message = IncomingMessage.builder()
                .channelMessageId(payload.getMessageSid())
                .channel(ChannelType.SMS)
                .sourceIdentifier(payload.getFrom())
                .sourceName(null)
                .type(MessageType.TEXT)
                .content(payload.getBody())
                .instanceId(instanceId)
                .timestamp(payload.getTimestamp())
                .build();

        return orchestrator.processMessage(message);
    }
}
