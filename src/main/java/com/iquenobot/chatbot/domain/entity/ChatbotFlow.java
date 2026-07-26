package com.iquenobot.chatbot.domain.entity;

import com.iquenobot.shared.common.SoftDeletableEntity;
import com.iquenobot.shared.enums.ChatbotFlowTrigger;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.ParamDef;

@Entity
@Table(name = "chatbot_flows")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotFlow extends SoftDeletableEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 30)
    private ChatbotFlowTrigger triggerType;

    @Column(name = "trigger_keywords", length = 1000)
    private String triggerKeywords; // JSON array of keywords

    @Column(name = "trigger_pattern", length = 500)
    private String triggerPattern; // Regex pattern

    @Column(name = "flow_config", columnDefinition = "TEXT", nullable = false)
    private String flowConfig; // JSON configuration of the flow

    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "use_ai", nullable = false)
    private boolean useAI = false;

    @Column(name = "ai_prompt", columnDefinition = "TEXT")
    private String aiPrompt;

    @Column(name = "fallback_message", columnDefinition = "TEXT")
    private String fallbackMessage;

    @Column(name = "success_count", nullable = false)
    private Long successCount = 0L;

    @Column(name = "failure_count", nullable = false)
    private Long failureCount = 0L;

    @Column(name = "execution_count", nullable = false)
    private Long executionCount = 0L;

    // Business methods
    public void incrementSuccess() {
        this.successCount++;
        this.executionCount++;
    }

    public void incrementFailure() {
        this.failureCount++;
        this.executionCount++;
    }

    public double getSuccessRate() {
        if (executionCount == 0) {
            return 0.0;
        }
        return (double) successCount / executionCount * 100;
    }
}
