package com.iquenobot.orchestrator.application.action;

import com.iquenobot.conversation.domain.entity.ConversationMessage;
import com.iquenobot.conversation.domain.repository.ConversationMessageRepository;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.ActionExecutor;
import com.iquenobot.orchestrator.domain.service.ChannelMessageSender;
import com.iquenobot.orchestrator.domain.service.EventPublisher;
import com.iquenobot.orchestrator.interfaces.event.BotAnsweredEvent;
import com.iquenobot.shared.application.MessageTemplateResolver;
import com.iquenobot.shared.enums.MessageDirection;
import com.iquenobot.shared.enums.MessageStatus;
import com.iquenobot.shared.enums.MessageType;
import com.iquenobot.shared.enums.SenderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SendTextExecutor implements ActionExecutor {

    private final ConversationMessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final EventPublisher eventPublisher;
    private final List<ChannelMessageSender> channelSenders;
    private final MessageTemplateResolver templateResolver;

    @Override
    public ActionType supportedActionType() { return ActionType.SEND_TEXT; }

    @Override
    @Transactional
    public void execute(Decision decision, ProcessingContext context) {
        String responseText = decision.getParameters() != null
                ? (String) decision.getParameters().get("response")
                : null;

        if (responseText == null || responseText.isBlank()) {
            log.warn("SendTextExecutor called but no response text in decision parameters");
            return;
        }

        responseText = templateResolver.resolve(responseText, context.getTenant(), context.getContact());

        String intentDetected = decision.getParameters() != null
                ? (String) decision.getParameters().getOrDefault("intent", "")
                : "";

        String channelMessageId = null;

        try {
            var channel = context.getIncomingMessage().getChannel();
            ChannelMessageSender sender = channelSenders.stream()
                    .filter(s -> s.supportedChannel() == channel)
                    .findFirst()
                    .orElse(null);

            if (sender != null) {
                String instanceId = context.getIncomingMessage().getInstanceId();
                String recipient = resolveRecipient(context);
                channelMessageId = sender.sendTextMessage(instanceId, recipient, responseText);
                log.info("Text message sent via {}: conversation={} recipient={}", channel, context.getConversation().getId(), recipient);
            } else {
                log.warn("No channel sender available for channel={}, message persisted but not delivered", channel);
            }
        } catch (Exception e) {
            log.error("Failed to send text message via channel: {}", e.getMessage(), e);
        }

        ConversationMessage botMessage = ConversationMessage.builder()
                .id(UUID.randomUUID())
                .tenantId(context.getTenantId())
                .conversation(context.getConversation())
                .direction(MessageDirection.OUTBOUND)
                .senderType(SenderType.BOT)
                .type(MessageType.TEXT)
                .status(channelMessageId != null ? MessageStatus.SENT : MessageStatus.FAILED)
                .content(responseText)
                .channelMessageId(channelMessageId)
                .fromBot(true)
                .botIntent(intentDetected)
                .sentAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        messageRepository.save(botMessage);

        var conv = context.getConversation();
        conversationRepository.incrementIncomingMessageMetrics(
                conv.getId(), context.getTenantId(), LocalDateTime.now(ZoneOffset.UTC));

        eventPublisher.publish(new BotAnsweredEvent(
                context.getTenantId().toString(),
                conv.getId().toString(),
                intentDetected,
                responseText.length() > 100 ? responseText.substring(0, 100) : responseText,
                false
        ));

        log.info("Bot text response processed: conversation={} intent={} delivered={}",
                conv.getId(), intentDetected, channelMessageId != null);
    }

    private String resolveRecipient(ProcessingContext context) {
        var channel = context.getIncomingMessage().getChannel();
        return switch (channel) {
            case WHATSAPP, SMS -> context.getContact().getPhone();
            default -> context.getIncomingMessage().getSourceIdentifier();
        };
    }
}
