package com.iquenobot.orchestrator.application.action;

import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.ActionExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WaitForResponseExecutor implements ActionExecutor {

    @Override
    public ActionType supportedActionType() { return ActionType.WAIT_FOR_RESPONSE; }

    @Override
    public void execute(Decision decision, ProcessingContext context) {
        log.info("Waiting for response: conversation={}",
                context.getConversation() != null ? context.getConversation().getId() : "unknown");
    }
}
