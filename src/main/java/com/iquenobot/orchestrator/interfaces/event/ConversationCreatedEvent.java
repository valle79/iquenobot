package com.iquenobot.orchestrator.interfaces.event;

public class ConversationCreatedEvent extends OrchestratorEvent {

    private final String contactId;
    private final String channel;

    public ConversationCreatedEvent(String tenantId, String conversationId, String contactId, String channel) {
        super(tenantId, conversationId);
        this.contactId = contactId;
        this.channel = channel;
    }

    public String getContactId() { return contactId; }
    public String getChannel() { return channel; }

    @Override
    public String getEventType() { return "CONVERSATION_CREATED"; }
}
