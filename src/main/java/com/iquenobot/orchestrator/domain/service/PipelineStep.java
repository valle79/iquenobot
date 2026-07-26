package com.iquenobot.orchestrator.domain.service;

import com.iquenobot.orchestrator.domain.model.ProcessingContext;

@FunctionalInterface
public interface PipelineStep {

    ProcessingContext execute(ProcessingContext context);
}
