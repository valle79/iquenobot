package com.iquenobot.orchestrator.interfaces.event;

import java.util.UUID;

public class BotAnsweredEvent extends OrchestratorEvent {

    private final UUID messageId;
    private final String intentName;
    private final String responsePreview;
    private final boolean usingAI;

    public BotAnsweredEvent(UUID messageId, String tenantId, String conversationId, String intentName, String responsePreview, boolean usingAI) {
        super(tenantId, conversationId);
        this.messageId = messageId;
        this.intentName = intentName;
        this.responsePreview = responsePreview;
        this.usingAI = usingAI;
    }

    public UUID getMessageId() { return messageId; }
    public String getIntentName() { return intentName; }
    public String getResponsePreview() { return responsePreview; }
    public boolean isUsingAI() { return usingAI; }

    @Override
    public String getEventType() { return "BOT_ANSWERED"; }
}
