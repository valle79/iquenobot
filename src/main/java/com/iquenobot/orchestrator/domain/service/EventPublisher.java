package com.iquenobot.orchestrator.domain.service;

import com.iquenobot.orchestrator.interfaces.event.OrchestratorEvent;

public interface EventPublisher {

    void publish(OrchestratorEvent event);
}
