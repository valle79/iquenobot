package com.iquenobot.orchestrator.domain.service;

import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;

public interface DecisionEngine {

    Decision decide(ProcessingContext context);
}
