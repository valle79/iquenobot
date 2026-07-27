package com.iquenobot.orchestrator.interfaces.event;

public class BotAnsweredEvent extends OrchestratorEvent {

    private final String intentName;
    private final String responsePreview;
    private final boolean usingAI;

    public BotAnsweredEvent(String tenantId, String conversationId, String intentName, String responsePreview, boolean usingAI) {
        super(tenantId, conversationId);
        this.intentName = intentName;
        this.responsePreview = responsePreview;
        this.usingAI = usingAI;
    }

    public String getIntentName() { return intentName; }
    public String getResponsePreview() { return responsePreview; }
    public boolean isUsingAI() { return usingAI; }

    @Override
    public String getEventType() { return "BOT_ANSWERED"; }
}
