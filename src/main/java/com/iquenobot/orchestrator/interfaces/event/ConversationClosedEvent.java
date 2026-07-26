package com.iquenobot.orchestrator.interfaces.event;

public class ConversationClosedEvent extends OrchestratorEvent {

    private final String reason;
    private final String closedBy;

    public ConversationClosedEvent(String tenantId, String conversationId, String reason, String closedBy) {
        super(tenantId, conversationId);
        this.reason = reason;
        this.closedBy = closedBy;
    }

    public String getReason() { return reason; }
    public String getClosedBy() { return closedBy; }

    @Override
    public String getEventType() { return "CONVERSATION_CLOSED"; }
}
