package com.iquenobot.orchestrator.interfaces.event;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class OrchestratorEvent {

    private final UUID eventId;
    private final LocalDateTime occurredAt;
    private final String tenantId;
    private final String conversationId;

    protected OrchestratorEvent(String tenantId, String conversationId) {
        this.eventId = UUID.randomUUID();
        this.occurredAt = LocalDateTime.now();
        this.tenantId = tenantId;
        this.conversationId = conversationId;
    }

    public UUID getEventId() { return eventId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getTenantId() { return tenantId; }
    public String getConversationId() { return conversationId; }

    public abstract String getEventType();
}
