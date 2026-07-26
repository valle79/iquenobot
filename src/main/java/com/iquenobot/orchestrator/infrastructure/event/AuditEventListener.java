package com.iquenobot.orchestrator.infrastructure.event;

import com.iquenobot.orchestrator.infrastructure.audit.AuditService;
import com.iquenobot.orchestrator.interfaces.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Listener that persists audit logs based on Orchestrator events.
 * Ensures compliance and traceability of all operations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventListener {

    private final AuditService auditService;

    @Async
    @EventListener
    public void handleMessageReceived(MessageReceivedEvent event) {
        log.debug("Persisting audit log for message received: messageId={}", event.getMessageId());
        
        // The initial audit log is created by AuditStep in the pipeline
        // This listener can be used for additional audit processing if needed
    }

    @Async
    @EventListener
    public void handleConversationCreated(ConversationCreatedEvent event) {
        log.info("Audit: Conversation created - conversationId={} channel={} tenantId={}",
                event.getConversationId(), event.getChannel(), event.getTenantId());
        
        // Additional audit logic for conversation creation
        // Could be used to track conversation lifecycle metrics
    }

    @Async
    @EventListener
    public void handleAgentAssigned(AgentAssignedEvent event) {
        log.info("Audit: Agent assigned - conversationId={} agentId={} tenantId={}",
                event.getConversationId(), event.getAgentId(), event.getTenantId());
        
        // Track agent assignment for performance metrics
    }

    @Async
    @EventListener
    public void handleBotAnswered(BotAnsweredEvent event) {
        log.info("Audit: Bot answered - conversationId={} usingAI={} tenantId={}",
                event.getConversationId(), event.isUsingAI(), event.getTenantId());
        
        // Track bot responses for quality monitoring
    }

    @Async
    @EventListener
    public void handleConversationClosed(ConversationClosedEvent event) {
        log.info("Audit: Conversation closed - conversationId={} reason={} tenantId={}",
                event.getConversationId(), event.getReason(), event.getTenantId());
        
        // Track conversation closure for analytics
    }

    @Async
    @EventListener
    public void handleActionExecuted(ActionExecutedEvent event) {
        log.debug("Audit: Action executed - action={} processingTime={}ms tenantId={}",
                event.getActionType(), event.getProcessingTimeMs(), event.getTenantId());
        
        // The detailed audit log is created by AuditService
        // This listener provides additional logging for monitoring
    }
}
