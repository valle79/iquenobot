package com.iquenobot.orchestrator.interfaces.event;

public class BotAnsweredEvent extends OrchestratorEvent {

    private final String intentName;
    private final String responsePreview;

    public BotAnsweredEvent(String tenantId, String conversationId, String intentName, String responsePreview) {
        super(tenantId, conversationId);
        this.intentName = intentName;
        this.responsePreview = responsePreview;
    }

    public String getIntentName() { return intentName; }
    public String getResponsePreview() { return responsePreview; }

    @Override
    public String getEventType() { return "BOT_ANSWERED"; }
}
