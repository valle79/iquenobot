package com.iquenobot.orchestrator.domain.entity;

import com.iquenobot.shared.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * Audit log entity for tracking Orchestrator operations.
 * Stores detailed information about message processing for compliance and debugging.
 */
@Entity
@Table(name = "orchestrator_audit_logs")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog extends BaseEntity {

    @Column(name = "message_id", length = 255)
    private String messageId;

    @Column(name = "conversation_id")
    private String conversationId;

    @Column(name = "contact_id")
    private String contactId;

    @Column(name = "channel", length = 20, nullable = false)
    private String channel;

    @Column(name = "event_type", length = 50, nullable = false)
    private String eventType; // MESSAGE_RECEIVED, DECISION_MADE, ACTION_EXECUTED, etc.

    @Column(name = "action_type", length = 50)
    private String actionType; // SEND_TEXT, ASSIGN_AGENT, CREATE_LEAD, etc.

    @Column(name = "decision_strategy", length = 100)
    private String decisionStrategy;

    @Column(name = "processing_time_ms", nullable = false)
    private Long processingTimeMs;

    @Column(name = "status", length = 20, nullable = false)
    private String status; // SUCCESS, FAILED, PARTIAL

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_type", length = 100)
    private String errorType;

    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData; // JSON of incoming message

    @Column(name = "output_data", columnDefinition = "TEXT")
    private String outputData; // JSON of processing result

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // Additional context as JSON

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;
}
