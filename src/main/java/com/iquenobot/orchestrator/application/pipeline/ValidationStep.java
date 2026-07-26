package com.iquenobot.orchestrator.application.pipeline;

import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.PipelineStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ValidationStep implements PipelineStep, MessagePipeline.PrioritizedStep {

    @Override
    public int getOrder() { return 0; }

    @Override
    public ProcessingContext execute(ProcessingContext context) {
        var message = context.getIncomingMessage();

        if (message == null) {
            throw new IllegalArgumentException("Incoming message cannot be null");
        }
        if (message.getChannel() == null) {
            throw new IllegalArgumentException("Message channel cannot be null");
        }
        if (message.getSourceIdentifier() == null || message.getSourceIdentifier().isBlank()) {
            throw new IllegalArgumentException("Message source identifier cannot be empty");
        }

        log.debug("Validation passed for message from channel={} source={}",
                message.getChannel(), message.getSourceIdentifier());

        return context;
    }
}
