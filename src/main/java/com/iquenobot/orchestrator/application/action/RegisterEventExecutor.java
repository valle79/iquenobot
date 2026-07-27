package com.iquenobot.orchestrator.application.action;

import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.ActionExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegisterEventExecutor implements ActionExecutor {

    @Override
    public ActionType supportedActionType() { return ActionType.REGISTER_EVENT; }

    @Override
    public void execute(Decision decision, ProcessingContext context) {
        Map<String, Object> params = decision.getParameters();
        String eventName = params != null ? (String) params.getOrDefault("eventName", "custom_event") : "custom_event";

        log.info("Event registered: name={} conversation={}",
                eventName, context.getConversation() != null ? context.getConversation().getId() : "unknown");
    }
}
