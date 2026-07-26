package com.iquenobot.orchestrator.interfaces.event;

import com.iquenobot.orchestrator.domain.model.IncomingMessage;

public class MessageReceivedEvent extends OrchestratorEvent {

    private final IncomingMessage message;

    public MessageReceivedEvent(String tenantId, String conversationId, IncomingMessage message) {
        super(tenantId, conversationId);
        this.message = message;
    }

    public IncomingMessage getMessage() { return message; }

    @Override
    public String getEventType() { return "MESSAGE_RECEIVED"; }
}
