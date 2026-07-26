package com.iquenobot.orchestrator.application.action;

import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.ActionExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NoopActionExecutor implements ActionExecutor {

    @Override
    public ActionType supportedActionType() { return ActionType.NO_ACTION; }

    @Override
    public void execute(Decision decision, ProcessingContext context) {
        log.debug("No action needed for conversation {}: {}",
                context.getConversation().getId(), decision.getReason());
    }
}
