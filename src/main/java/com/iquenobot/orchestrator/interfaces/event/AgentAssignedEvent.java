package com.iquenobot.orchestrator.interfaces.event;

public class AgentAssignedEvent extends OrchestratorEvent {

    private final String agentId;
    private final String contactId;

    public AgentAssignedEvent(String tenantId, String conversationId, String agentId, String contactId) {
        super(tenantId, conversationId);
        this.agentId = agentId;
        this.contactId = contactId;
    }

    public String getAgentId() { return agentId; }
    public String getContactId() { return contactId; }

    @Override
    public String getEventType() { return "AGENT_ASSIGNED"; }
}
