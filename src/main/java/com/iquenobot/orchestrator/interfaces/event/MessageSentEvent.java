package com.iquenobot.orchestrator.interfaces.event;

public class MessageSentEvent extends OrchestratorEvent {

    private final String messageId;
    private final String content;
    private final String direction;
    private final String type;
    private final String senderName;

    public MessageSentEvent(String tenantId, String conversationId, String messageId,
                            String content, String direction, String type, String senderName) {
        super(tenantId, conversationId);
        this.messageId = messageId;
        this.content = content;
        this.direction = direction;
        this.type = type;
        this.senderName = senderName;
    }

    public String getMessageId() { return messageId; }
    public String getContent() { return content; }
    public String getDirection() { return direction; }
    public String getType() { return type; }
    public String getSenderName() { return senderName; }

    @Override
    public String getEventType() { return "MESSAGE_SENT"; }
}
