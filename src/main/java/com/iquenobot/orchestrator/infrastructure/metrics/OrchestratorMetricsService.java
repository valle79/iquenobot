package com.iquenobot.orchestrator.infrastructure.metrics;

import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.shared.enums.ChannelType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for tracking Orchestrator metrics using Micrometer.
 * Provides real-time observability for message processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrchestratorMetricsService {

    private final MeterRegistry meterRegistry;

    /**
     * Record successful message processing
     */
    public void recordMessageProcessed(ChannelType channel, ActionType action, long processingTimeMs) {
        // Increment success counter
        Counter.builder("orchestrator.messages.processed")
                .tag("channel", channel.name())
                .tag("action", action.name())
                .tag("status", "success")
                .description("Total messages successfully processed")
                .register(meterRegistry)
                .increment();

        // Record processing time
        Timer.builder("orchestrator.processing.time")
                .tag("channel", channel.name())
                .tag("action", action.name())
                .description("Message processing time in milliseconds")
                .register(meterRegistry)
                .record(processingTimeMs, TimeUnit.MILLISECONDS);

        log.debug("Metrics recorded: channel={} action={} time={}ms", channel, action, processingTimeMs);
    }

    /**
     * Record failed message processing
     */
    public void recordMessageFailed(ChannelType channel, String errorType) {
        Counter.builder("orchestrator.messages.failed")
                .tag("channel", channel.name())
                .tag("error_type", errorType)
                .description("Total messages that failed processing")
                .register(meterRegistry)
                .increment();

        log.debug("Failure recorded: channel={} error={}", channel, errorType);
    }

    /**
     * Record decision made by Decision Engine
     */
    public void recordDecision(ActionType decision, String strategy) {
        Counter.builder("orchestrator.decisions")
                .tag("decision", decision.name())
                .tag("strategy", strategy)
                .description("Decisions made by the Decision Engine")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record pipeline step execution
     */
    public void recordPipelineStep(String stepName, long executionTimeMs) {
        Timer.builder("orchestrator.pipeline.step")
                .tag("step", stepName)
                .description("Pipeline step execution time")
                .register(meterRegistry)
                .record(executionTimeMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record action execution
     */
    public void recordActionExecuted(ActionType actionType, boolean success) {
        Counter.builder("orchestrator.actions.executed")
                .tag("action", actionType.name())
                .tag("status", success ? "success" : "failed")
                .description("Actions executed by Action Executors")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record tenant activity
     */
    public void recordTenantActivity(String tenantId) {
        Counter.builder("orchestrator.tenant.activity")
                .tag("tenant_id", tenantId)
                .description("Activity per tenant")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record channel webhook received
     */
    public void recordWebhookReceived(ChannelType channel) {
        Counter.builder("orchestrator.webhooks.received")
                .tag("channel", channel.name())
                .description("Webhooks received per channel")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record bot response generated
     */
    public void recordBotResponse(boolean usingAI) {
        Counter.builder("orchestrator.bot.responses")
                .tag("type", usingAI ? "ai" : "template")
                .description("Bot responses generated")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record agent assignment
     */
    public void recordAgentAssignment(String assignmentType) {
        Counter.builder("orchestrator.agent.assignments")
                .tag("type", assignmentType)
                .description("Agent assignments")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record lead creation
     */
    public void recordLeadCreated(String source) {
        Counter.builder("orchestrator.leads.created")
                .tag("source", source)
                .description("Leads created from conversations")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record conversation closed
     */
    public void recordConversationClosed(String reason) {
        Counter.builder("orchestrator.conversations.closed")
                .tag("reason", reason)
                .description("Conversations closed")
                .register(meterRegistry)
                .increment();
    }
}
