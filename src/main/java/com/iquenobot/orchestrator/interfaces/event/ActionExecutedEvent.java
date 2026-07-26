package com.iquenobot.orchestrator.interfaces.event;

import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;

public class ActionExecutedEvent extends OrchestratorEvent {

    private final Decision decision;
    private final IncomingMessage sourceMessage;
    private final long processingTimeMs;

    public ActionExecutedEvent(String tenantId, String conversationId,
                               Decision decision, IncomingMessage sourceMessage,
                               long processingTimeMs) {
        super(tenantId, conversationId);
        this.decision = decision;
        this.sourceMessage = sourceMessage;
        this.processingTimeMs = processingTimeMs;
    }

    public Decision getDecision() { return decision; }
    public ActionType getActionType() { return decision.getActionType(); }
    public IncomingMessage getSourceMessage() { return sourceMessage; }
    public long getProcessingTimeMs() { return processingTimeMs; }

    @Override
    public String getEventType() { return "ACTION_EXECUTED"; }
}
