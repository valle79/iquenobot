package com.iquenobot.orchestrator.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Domain model that holds conversation-specific state for the Decision Engine.
 * Separate from ProcessingContext — this captures the conversation's history and session data.
 */
public class ConversationContext {

    private final int totalMessages;
    private final int messagesFromContact;
    private final int messagesFromBot;
    private final LocalDateTime lastBotResponseAt;
    private final LocalDateTime lastContactMessageAt;
    private final List<String> recentIntents;
    private final Map<String, Object> sessionData;
    private final boolean firstMessageInConversation;
    private final long secondsSinceLastMessage;
    private final int consecutiveBotResponses;

    private ConversationContext(Builder builder) {
        this.totalMessages = builder.totalMessages;
        this.messagesFromContact = builder.messagesFromContact;
        this.messagesFromBot = builder.messagesFromBot;
        this.lastBotResponseAt = builder.lastBotResponseAt;
        this.lastContactMessageAt = builder.lastContactMessageAt;
        this.recentIntents = builder.recentIntents;
        this.sessionData = builder.sessionData;
        this.firstMessageInConversation = builder.firstMessageInConversation;
        this.secondsSinceLastMessage = builder.secondsSinceLastMessage;
        this.consecutiveBotResponses = builder.consecutiveBotResponses;
    }

    public int getTotalMessages() { return totalMessages; }
    public int getMessagesFromContact() { return messagesFromContact; }
    public int getMessagesFromBot() { return messagesFromBot; }
    public LocalDateTime getLastBotResponseAt() { return lastBotResponseAt; }
    public LocalDateTime getLastContactMessageAt() { return lastContactMessageAt; }
    public List<String> getRecentIntents() { return recentIntents; }
    public Map<String, Object> getSessionData() { return sessionData; }
    public boolean isFirstMessageInConversation() { return firstMessageInConversation; }
    public long getSecondsSinceLastMessage() { return secondsSinceLastMessage; }
    public int getConsecutiveBotResponses() { return consecutiveBotResponses; }

    public boolean hasExceededBotResponseLimit(int maxConsecutive) {
        return consecutiveBotResponses >= maxConsecutive;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ConversationContext empty() {
        return builder().firstMessageInConversation(true).build();
    }

    public static class Builder {
        private int totalMessages = 0;
        private int messagesFromContact = 0;
        private int messagesFromBot = 0;
        private LocalDateTime lastBotResponseAt;
        private LocalDateTime lastContactMessageAt;
        private List<String> recentIntents = List.of();
        private Map<String, Object> sessionData = Map.of();
        private boolean firstMessageInConversation = true;
        private long secondsSinceLastMessage = 0;
        private int consecutiveBotResponses = 0;

        public Builder totalMessages(int totalMessages) { this.totalMessages = totalMessages; return this; }
        public Builder messagesFromContact(int messagesFromContact) { this.messagesFromContact = messagesFromContact; return this; }
        public Builder messagesFromBot(int messagesFromBot) { this.messagesFromBot = messagesFromBot; return this; }
        public Builder lastBotResponseAt(LocalDateTime lastBotResponseAt) { this.lastBotResponseAt = lastBotResponseAt; return this; }
        public Builder lastContactMessageAt(LocalDateTime lastContactMessageAt) { this.lastContactMessageAt = lastContactMessageAt; return this; }
        public Builder recentIntents(List<String> recentIntents) { this.recentIntents = recentIntents; return this; }
        public Builder sessionData(Map<String, Object> sessionData) { this.sessionData = sessionData; return this; }
        public Builder firstMessageInConversation(boolean firstMessageInConversation) { this.firstMessageInConversation = firstMessageInConversation; return this; }
        public Builder secondsSinceLastMessage(long secondsSinceLastMessage) { this.secondsSinceLastMessage = secondsSinceLastMessage; return this; }
        public Builder consecutiveBotResponses(int consecutiveBotResponses) { this.consecutiveBotResponses = consecutiveBotResponses; return this; }

        public ConversationContext build() {
            return new ConversationContext(this);
        }
    }
}
