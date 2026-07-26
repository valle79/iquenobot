package com.iquenobot.orchestrator.infrastructure.event;

import com.iquenobot.orchestrator.domain.service.EventPublisher;
import com.iquenobot.orchestrator.interfaces.event.OrchestratorEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpringEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher springPublisher;

    @Override
    public void publish(OrchestratorEvent event) {
        log.debug("Publishing event: type={} tenantId={} conversationId={}",
                event.getEventType(), event.getTenantId(), event.getConversationId());
        springPublisher.publishEvent(event);
    }
}
