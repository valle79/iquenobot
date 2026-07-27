package com.iquenobot.orchestrator.infrastructure.event;

import com.iquenobot.orchestrator.infrastructure.metrics.OrchestratorMetricsService;
import com.iquenobot.orchestrator.interfaces.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for analytics tracking based on Orchestrator events.
 * Processes events asynchronously to avoid blocking the main flow.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsEventListener {

    private final OrchestratorMetricsService metricsService;

    @Async
    @EventListener
    public void handleMessageReceived(MessageReceivedEvent event) {
        log.info("Analytics: Message received - tenantId={} conversationId={} channel={}",
                event.getTenantId(), event.getConversationId(), event.getChannel());

        // Track webhook received
        metricsService.recordWebhookReceived(event.getChannelType());

        // Track tenant activity
        if (event.getTenantId() != null) {
            metricsService.recordTenantActivity(event.getTenantId());
        }

        // TODO: Send to analytics platform (Google Analytics, Mixpanel, Amplitude, etc.)
        // analyticsClient.trackEvent("message_received", event.toMap());
    }

    @Async
    @EventListener
    public void handleConversationCreated(ConversationCreatedEvent event) {
        log.info("Analytics: Conversation created - tenantId={} conversationId={} channel={}",
                event.getTenantId(), event.getConversationId(), event.getChannel());

        // TODO: Track conversation creation
        // analyticsClient.trackEvent("conversation_created", event.toMap());
    }

    @Async
    @EventListener
    public void handleAgentAssigned(AgentAssignedEvent event) {
        log.info("Analytics: Agent assigned - tenantId={} conversationId={} agentId={}",
                event.getTenantId(), event.getConversationId(), event.getAgentId());

        metricsService.recordAgentAssignment("manual");

        // TODO: Track agent assignment
        // analyticsClient.trackEvent("agent_assigned", event.toMap());
    }

    @Async
    @EventListener
    public void handleBotAnswered(BotAnsweredEvent event) {
        log.info("Analytics: Bot answered - tenantId={} conversationId={} usingAI={}",
                event.getTenantId(), event.getConversationId(), event.isUsingAI());

        metricsService.recordBotResponse(event.isUsingAI());

        // TODO: Track bot response
        // analyticsClient.trackEvent("bot_answered", event.toMap());
    }

    @Async
    @EventListener
    public void handleConversationClosed(ConversationClosedEvent event) {
        log.info("Analytics: Conversation closed - tenantId={} conversationId={} reason={}",
                event.getTenantId(), event.getConversationId(), event.getReason());

        metricsService.recordConversationClosed(event.getReason());

        // TODO: Track conversation closure
        // analyticsClient.trackEvent("conversation_closed", event.toMap());
    }

    @Async
    @EventListener
    public void handleActionExecuted(ActionExecutedEvent event) {
        log.debug("Analytics: Action executed - tenantId={} conversationId={} action={} time={}ms",
                event.getTenantId(), event.getConversationId(),
                event.getActionType(), event.getProcessingTimeMs());

        // Track action execution
        metricsService.recordActionExecuted(event.getActionType(), true);

        // Track processing time and decision
        if (event.getSourceMessage() != null) {
            metricsService.recordMessageProcessed(
                    event.getSourceMessage().getChannel(),
                    event.getActionType(),
                    event.getProcessingTimeMs()
            );
        }

        // TODO: Send to analytics platform
        // analyticsClient.trackEvent("action_executed", event.toMap());
    }
}
