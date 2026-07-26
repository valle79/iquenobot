package com.iquenobot.orchestrator.application.action;

import com.iquenobot.conversation.domain.entity.ConversationMessage;
import com.iquenobot.conversation.domain.repository.ConversationMessageRepository;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.ActionExecutor;
import com.iquenobot.orchestrator.interfaces.event.BotAnsweredEvent;
import com.iquenobot.orchestrator.domain.service.EventPublisher;
import com.iquenobot.shared.enums.MessageDirection;
import com.iquenobot.shared.enums.MessageStatus;
import com.iquenobot.shared.enums.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SendTextExecutor implements ActionExecutor {

    private final ConversationMessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final EventPublisher eventPublisher;

    @Override
    public ActionType supportedActionType() { return ActionType.SEND_TEXT; }

    @Override
    public void execute(Decision decision, ProcessingContext context) {
        String responseText = decision.getParameters() != null
                ? (String) decision.getParameters().get("response")
                : null;

        if (responseText == null || responseText.isBlank()) {
            log.warn("SendTextExecutor called but no response text in decision parameters");
            return;
        }

        String intentDetected = decision.getParameters() != null
                ? (String) decision.getParameters().getOrDefault("intent", "")
                : "";

        ConversationMessage botMessage = ConversationMessage.builder()
                .id(UUID.randomUUID())
                .tenantId(context.getTenantId())
                .conversation(context.getConversation())
                .direction(MessageDirection.OUTBOUND)
                .type(MessageType.TEXT)
                .status(MessageStatus.SENT)
                .content(responseText)
                .fromBot(true)
                .botIntent(intentDetected)
                .sentAt(LocalDateTime.now())
                .build();

        messageRepository.save(botMessage);

        var conv = context.getConversation();
        conv.incrementMessageCount();
        conv.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conv);

        eventPublisher.publish(new BotAnsweredEvent(
                context.getTenantId().toString(),
                conv.getId().toString(),
                intentDetected,
                responseText.length() > 100 ? responseText.substring(0, 100) : responseText
        ));

        log.info("Bot text response sent: conversation={} intent={}",
                conv.getId(), intentDetected);
    }
}
