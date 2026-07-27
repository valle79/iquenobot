package com.iquenobot.orchestrator.interfaces.event;

import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.shared.enums.ChannelType;

public class MessageReceivedEvent extends OrchestratorEvent {

    private final IncomingMessage message;

    public MessageReceivedEvent(String tenantId, String conversationId, IncomingMessage message) {
        super(tenantId, conversationId);
        this.message = message;
    }

    public IncomingMessage getMessage() { return message; }
    public ChannelType getChannelType() { return message != null ? message.getChannel() : null; }
    public String getChannel() { return message != null ? message.getChannel().name() : "UNKNOWN"; }
    public String getMessageId() { return message != null ? message.getChannelMessageId() : null; }

    @Override
    public String getEventType() { return "MESSAGE_RECEIVED"; }
}
