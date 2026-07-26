package com.iquenobot.orchestrator.application.decision;

import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.DecisionStrategy;
import com.iquenobot.shared.enums.ConversationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AgentAssignmentStrategy implements DecisionStrategy {

    @Override
    public int getPriority() { return 30; }

    @Override
    public boolean canHandle(ProcessingContext context) {
        return context.getConversation().isUnassigned()
                && context.getConversation().getStatus() == ConversationStatus.OPEN;
    }

    @Override
    public Decision decide(ProcessingContext context) {
        log.debug("No agent assigned and conversation is OPEN, assigning agent");

        return Decision.builder()
                .actionType(ActionType.ASSIGN_AGENT)
                .reason("Conversation requires human agent")
                .requiresAgent(true)
                .build();
    }
}
