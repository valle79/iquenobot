package com.iquenobot.orchestrator.infrastructure.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iquenobot.orchestrator.domain.entity.AuditLog;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.model.ProcessingResult;
import com.iquenobot.orchestrator.domain.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for persisting audit logs of Orchestrator operations.
 * Provides compliance tracking and debugging capabilities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Log message received event
     */
    @Async
    @Transactional
    public void logMessageReceived(IncomingMessage message, UUID tenantId) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .tenantId(tenantId)
                    .messageId(message.getChannelMessageId())
                    .conversationId(null)
                    .contactId(message.getSourceIdentifier())
                    .channel(message.getChannel().name())
                    .eventType("MESSAGE_RECEIVED")
                    .processingTimeMs(0L)
                    .status("PENDING")
                    .inputData(serializeToJson(message))
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created for message received: messageId={}", message.getChannelMessageId());

        } catch (Exception e) {
            log.error("Error creating audit log for message received", e);
        }
    }

    /**
     * Log decision made event
     */
    @Async
    @Transactional
    public void logDecisionMade(ProcessingContext context, Decision decision, String strategy) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("strategy", strategy);
            metadata.put("confidence", decision.getConfidence());
            if (decision.getParameters() != null) {
                metadata.put("parameters", decision.getParameters());
            }

            AuditLog auditLog = AuditLog.builder()
                    .tenantId(context.getTenantId())
                    .messageId(context.getIncomingMessage() != null ? 
                              context.getIncomingMessage().getChannelMessageId() : null)
                    .conversationId(context.getConversation() != null ? 
                                   context.getConversation().getId().toString() : null)
                    .contactId(context.getContact() != null ? 
                              context.getContact().getId().toString() : null)
                    .channel(context.getIncomingMessage() != null ? 
                            context.getIncomingMessage().getChannel().name() : "UNKNOWN")
                    .eventType("DECISION_MADE")
                    .actionType(decision.getActionType().name())
                    .decisionStrategy(strategy)
                    .processingTimeMs(0L)
                    .status("SUCCESS")
                    .metadata(serializeToJson(metadata))
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created for decision made: action={}", decision.getActionType());

        } catch (Exception e) {
            log.error("Error creating audit log for decision made", e);
        }
    }

    /**
     * Log action executed event
     */
    @Async
    @Transactional
    public void logActionExecuted(ProcessingContext context, Decision decision, 
                                  ProcessingResult result, long processingTimeMs) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .tenantId(context.getTenantId())
                    .messageId(context.getIncomingMessage() != null ? 
                              context.getIncomingMessage().getChannelMessageId() : null)
                    .conversationId(context.getConversation() != null ? 
                                   context.getConversation().getId().toString() : null)
                    .contactId(context.getContact() != null ? 
                              context.getContact().getId().toString() : null)
                    .channel(context.getIncomingMessage() != null ? 
                            context.getIncomingMessage().getChannel().name() : "UNKNOWN")
                    .eventType("ACTION_EXECUTED")
                    .actionType(decision.getActionType().name())
                    .processingTimeMs(processingTimeMs)
                    .status(result.isSuccess() ? "SUCCESS" : "FAILED")
                    .errorMessage(result.isSuccess() ? null : result.getMessage())
                    .outputData(serializeToJson(result))
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created for action executed: action={} status={}", 
                     decision.getActionType(), auditLog.getStatus());

        } catch (Exception e) {
            log.error("Error creating audit log for action executed", e);
        }
    }

    /**
     * Log processing error
     */
    @Async
    @Transactional
    public void logProcessingError(IncomingMessage message, UUID tenantId, 
                                   Exception error, long processingTimeMs) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .tenantId(tenantId)
                    .messageId(message.getChannelMessageId())
                    .conversationId(null)
                    .contactId(message.getSourceIdentifier())
                    .channel(message.getChannel().name())
                    .eventType("PROCESSING_ERROR")
                    .processingTimeMs(processingTimeMs)
                    .status("FAILED")
                    .errorType(error.getClass().getSimpleName())
                    .errorMessage(error.getMessage())
                    .inputData(serializeToJson(message))
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created for processing error: messageId={}", message.getChannelMessageId());

        } catch (Exception e) {
            log.error("Error creating audit log for processing error", e);
        }
    }

    /**
     * Serialize object to JSON string
     */
    private String serializeToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.warn("Error serializing object to JSON", e);
            return "{}";
        }
    }
}
