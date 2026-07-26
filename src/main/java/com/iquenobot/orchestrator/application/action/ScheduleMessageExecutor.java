package com.iquenobot.orchestrator.application.action;

import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.ActionExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ScheduleMessageExecutor implements ActionExecutor {

    @Override
    public ActionType supportedActionType() { return ActionType.SCHEDULE_MESSAGE; }

    @Override
    public void execute(Decision decision, ProcessingContext context) {
        var params = decision.getParameters();
        String scheduledMessage = params != null ? (String) params.get("message") : null;
        String scheduledTime = params != null ? (String) params.get("scheduledAt") : null;

        log.info("Message scheduled: conversation={} message={} at={}",
                context.getConversation().getId(), scheduledMessage, scheduledTime);
    }
}
