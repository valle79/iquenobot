package com.iquenobot.orchestrator.domain.service;

import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;

public interface ActionExecutor {

    void execute(Decision decision, ProcessingContext context);

    ActionType supportedActionType();
}
