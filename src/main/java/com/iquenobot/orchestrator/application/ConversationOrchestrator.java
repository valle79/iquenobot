package com.iquenobot.orchestrator.application;

import com.iquenobot.orchestrator.application.action.ActionDispatcher;
import com.iquenobot.orchestrator.application.pipeline.MessagePipeline;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.model.ProcessingResult;
import com.iquenobot.orchestrator.domain.service.DecisionEngine;
import com.iquenobot.orchestrator.domain.service.EventPublisher;
import com.iquenobot.orchestrator.interfaces.event.ActionExecutedEvent;
import com.iquenobot.orchestrator.interfaces.event.MessageReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationOrchestrator {

    private final MessagePipeline pipeline;
    private final DecisionEngine decisionEngine;
    private final ActionDispatcher actionDispatcher;
    private final EventPublisher eventPublisher;

    @Transactional
    public ProcessingResult processMessage(IncomingMessage message) {
        long startTime = System.currentTimeMillis();
        log.info("Orchestrator processing message from channel={} source={}",
                message.getChannel(), message.getSourceIdentifier());

        try {
            ProcessingContext context = new ProcessingContext();
            context.setIncomingMessage(message);

            context = pipeline.execute(context);

            eventPublisher.publish(new MessageReceivedEvent(
                    context.getTenantId() != null ? context.getTenantId().toString() : null,
                    context.getConversation() != null ? context.getConversation().getId().toString() : null,
                    message
            ));

            Decision decision = decisionEngine.decide(context);

            actionDispatcher.dispatch(decision, context);

            eventPublisher.publish(new ActionExecutedEvent(
                    context.getTenantId() != null ? context.getTenantId().toString() : null,
                    context.getConversation() != null ? context.getConversation().getId().toString() : null,
                    decision,
                    message,
                    System.currentTimeMillis() - startTime
            ));

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Message processed successfully in {}ms. tenantId={} conversationId={} decision={}",
                    elapsed,
                    context.getTenantId(),
                    context.getConversation() != null ? context.getConversation().getId() : null,
                    decision.getActionType());

            return ProcessingResult.success(decision, "Message processed successfully", elapsed);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Message processing failed after {}ms: {}",
                    elapsed, e.getMessage(), e);
            return ProcessingResult.failure("PROCESSING_ERROR", e.getMessage(), elapsed);
        }
    }
}
