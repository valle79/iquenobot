package com.iquenobot.orchestrator.domain.model;

import java.util.Map;

public class Decision {

    private final ActionType actionType;
    private final Map<String, Object> parameters;
    private final String reason;
    private final boolean requiresAgent;

    private Decision(Builder builder) {
        this.actionType = builder.actionType;
        this.parameters = builder.parameters;
        this.reason = builder.reason;
        this.requiresAgent = builder.requiresAgent;
    }

    public ActionType getActionType() { return actionType; }
    public Map<String, Object> getParameters() { return parameters; }
    public String getReason() { return reason; }
    public boolean isRequiresAgent() { return requiresAgent; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ActionType actionType;
        private Map<String, Object> parameters;
        private String reason;
        private boolean requiresAgent;

        public Builder actionType(ActionType actionType) { this.actionType = actionType; return this; }
        public Builder parameters(Map<String, Object> parameters) { this.parameters = parameters; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder requiresAgent(boolean requiresAgent) { this.requiresAgent = requiresAgent; return this; }

        public Decision build() {
            return new Decision(this);
        }
    }
}
