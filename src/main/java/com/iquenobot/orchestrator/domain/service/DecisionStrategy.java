package com.iquenobot.orchestrator.domain.service;

import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;

public interface DecisionStrategy {

    int getPriority();

    boolean canHandle(ProcessingContext context);

    Decision decide(ProcessingContext context);
}
