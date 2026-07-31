package com.iquenobot.orchestrator.interfaces.event;

public class ConversationCreatedEvent extends OrchestratorEvent {

    private final String contactId;
    private final String channel;
    private final String contactName;

    public ConversationCreatedEvent(String tenantId, String conversationId, String contactId, String channel, String contactName) {
        super(tenantId, conversationId);
        this.contactId = contactId;
        this.channel = channel;
        this.contactName = contactName;
    }

    public String getContactId() { return contactId; }
    public String getChannel() { return channel; }
    public String getContactName() { return contactName; }

    @Override
    public String getEventType() { return "CONVERSATION_CREATED"; }
}
