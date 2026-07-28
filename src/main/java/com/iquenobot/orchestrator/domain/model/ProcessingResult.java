package com.iquenobot.orchestrator.domain.model;

public class ProcessingResult {

    private final boolean success;
    private final String message;
    private final Decision decision;
    private final String errorCode;
    private final long processingTimeMs;

    private ProcessingResult(Builder builder) {
        this.success = builder.success;
        this.message = builder.message;
        this.decision = builder.decision;
        this.errorCode = builder.errorCode;
        this.processingTimeMs = builder.processingTimeMs;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Decision getDecision() { return decision; }
    public String getErrorCode() { return errorCode; }
    public long getProcessingTimeMs() { return processingTimeMs; }

    public static Builder builder() {
        return new Builder();
    }

    public static ProcessingResult success(Decision decision, String message, long processingTimeMs) {
        return new Builder()
                .success(true)
                .message(message)
                .decision(decision)
                .processingTimeMs(processingTimeMs)
                .build();
    }

    public static ProcessingResult empty() {
        return new Builder()
                .success(true)
                .message("skipped")
                .build();
    }

    public static ProcessingResult failure(String errorCode, String message, long processingTimeMs) {
        return new Builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .processingTimeMs(processingTimeMs)
                .build();
    }

    public static class Builder {
        private boolean success;
        private String message;
        private Decision decision;
        private String errorCode;
        private long processingTimeMs;

        public Builder success(boolean success) { this.success = success; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder decision(Decision decision) { this.decision = decision; return this; }
        public Builder errorCode(String errorCode) { this.errorCode = errorCode; return this; }
        public Builder processingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; return this; }

        public ProcessingResult build() {
            return new ProcessingResult(this);
        }
    }
}
