package com.iquenobot.chatbot.domain.entity;

import com.iquenobot.shared.common.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.ParamDef;

@Entity
@Table(name = "chatbot_intents")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotIntent extends SoftDeletableEntity {

    @Column(name = "intent_name", nullable = false, length = 100)
    private String intentName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "training_phrases", columnDefinition = "TEXT", nullable = false)
    private String trainingPhrases; // JSON array of training phrases

    @Column(name = "responses", columnDefinition = "TEXT", nullable = false)
    private String responses; // JSON array of possible responses

    @Column(name = "entities", columnDefinition = "TEXT")
    private String entities; // JSON array of entity types to extract

    @Column(name = "context_required", length = 500)
    private String contextRequired; // JSON array of required contexts

    @Column(name = "context_output", length = 500)
    private String contextOutput; // JSON array of contexts to set

    @Column(name = "actions", columnDefinition = "TEXT")
    private String actions; // JSON array of actions to execute

    @Column(name = "confidence_threshold", nullable = false)
    private Double confidenceThreshold = 0.7;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    @Column(name = "matched_count", nullable = false)
    private Long matchedCount = 0L;

    // Business methods
    public void incrementMatchedCount() {
        this.matchedCount++;
    }
}
