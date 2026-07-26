package com.iquenobot.orchestrator.application.action;

import com.iquenobot.conversation.domain.entity.Conversation;
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
public class AssignAgentExecutor implements ActionExecutor {

    private final ConversationRepository conversationRepository;
    private final EventPublisher eventPublisher;

    @Override
    public ActionType supportedActionType() { return ActionType.ASSIGN_AGENT; }

    @Override
    public void execute(Decision decision, ProcessingContext context) {
        Conversation conv = context.getConversation();

        if (conv.getAssignedUser() != null) {
            log.debug("Conversation {} already assigned to agent {}", conv.getId(), conv.getAssignedUser().getId());
            return;
        }

        conv.setStatus(ConversationStatus.IN_PROGRESS);
        conversationRepository.save(conv);

        eventPublisher.publish(new AgentAssignedEvent(
                context.getTenantId().toString(),
                conv.getId().toString(),
                null,
                context.getContact().getId().toString()
        ));

        log.info("Conversation {} marked for agent assignment", conv.getId());
    }
}
