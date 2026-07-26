package com.iquenobot.orchestrator.application.pipeline;

import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.PipelineStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class MessagePipeline {

    private final List<PipelineStep> steps;

    public MessagePipeline(List<PipelineStep> steps) {
        this.steps = steps.stream()
                .sorted((a, b) -> {
                    int p1 = a instanceof PrioritizedStep ? ((PrioritizedStep) a).getOrder() : 100;
                    int p2 = b instanceof PrioritizedStep ? ((PrioritizedStep) b).getOrder() : 100;
                    return Integer.compare(p1, p2);
                })
                .toList();
    }

    public ProcessingContext execute(ProcessingContext context) {
        log.info("Starting message pipeline with {} steps for channel: {}",
                steps.size(), context.getIncomingMessage().getChannel());

        long startTime = System.currentTimeMillis();

        for (PipelineStep step : steps) {
            String stepName = step.getClass().getSimpleName();
            long stepStart = System.currentTimeMillis();

            try {
                context = step.execute(context);
                long elapsed = System.currentTimeMillis() - stepStart;
                log.debug("Pipeline step {} completed in {}ms", stepName, elapsed);
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - stepStart;
                log.error("Pipeline step {} failed after {}ms: {}", stepName, elapsed, e.getMessage(), e);
                throw new PipelineExecutionException("Step " + stepName + " failed: " + e.getMessage(), e);
            }
        }

        long totalElapsed = System.currentTimeMillis() - startTime;
        log.info("Message pipeline completed in {}ms", totalElapsed);

        return context;
    }

    public interface PrioritizedStep {
        int getOrder();
    }

    public static class PipelineExecutionException extends RuntimeException {
        public PipelineExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
