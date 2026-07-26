package com.iquenobot.orchestrator.application.action;

import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.ActionExecutor;
import com.iquenobot.orchestrator.interfaces.event.AgentAssignedEvent;
import com.iquenobot.orchestrator.domain.service.EventPublisher;
import com.iquenobot.shared.enums.ConversationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferConversationExecutor implements ActionExecutor {

    private final ConversationRepository conversationRepository;
    private final EventPublisher eventPublisher;

    @Override
    public ActionType supportedActionType() { return ActionType.TRANSFER_CONVERSATION; }

    @Override
    public void execute(Decision decision, ProcessingContext context) {
        var conv = context.getConversation();

        conv.handoffFromBot();
        conversationRepository.save(conv);

        eventPublisher.publish(new AgentAssignedEvent(
                context.getTenantId().toString(),
                conv.getId().toString(),
                null,
                context.getContact().getId().toString()
        ));

        log.info("Conversation {} transferred from bot to human queue", conv.getId());
    }
}
