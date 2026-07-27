package com.iquenobot.orchestrator.application.action;

import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.ActionExecutor;
import com.iquenobot.orchestrator.domain.service.EventPublisher;
import com.iquenobot.orchestrator.interfaces.event.AgentAssignedEvent;
import com.iquenobot.shared.enums.ConversationPriority;
import com.iquenobot.shared.enums.ConversationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class EscalateSupervisorExecutor implements ActionExecutor {

    private final ConversationRepository conversationRepository;
    private final EventPublisher eventPublisher;

    @Override
    public ActionType supportedActionType() { return ActionType.ESCALATE_SUPERVISOR; }

    @Override
    @Transactional
    public void execute(Decision decision, ProcessingContext context) {
        Conversation conv = context.getConversation();

        conv.setStatus(ConversationStatus.OPEN);
        conv.setPriority(ConversationPriority.HIGH);
        conversationRepository.save(conv);

        eventPublisher.publish(new AgentAssignedEvent(
                context.getTenantId().toString(),
                conv.getId().toString(),
                null,
                context.getContact().getId().toString()
        ));

        log.info("Conversation escalated to supervisor: id={} reason={}",
                conv.getId(), decision.getReason());
    }
}
