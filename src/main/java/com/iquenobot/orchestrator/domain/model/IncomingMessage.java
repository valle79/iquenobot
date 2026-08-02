package com.iquenobot.orchestrator.domain.model;

import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.MessageType;

import java.time.LocalDateTime;
import java.util.Map;

public class IncomingMessage {

    private final String channelMessageId;
    private final ChannelType channel;
    private final String channelConversationId;
    private final String sourceIdentifier;
    private final String sourceName;
    private final MessageType type;
    private final String content;
    private final String mediaUrl;
    private final String caption;
    private final String filename;
    private final String mimeType;
    private final String channelMediaId;
    private final Integer durationSeconds;
    private final LocalDateTime timestamp;
    private final String tenantId;
    private final String instanceId;
    private final String conversationName;
    private final Map<String, Object> metadata;
    private final boolean outbound;
    private final boolean group;

    private IncomingMessage(Builder builder) {
        this.channelMessageId = builder.channelMessageId;
        this.channel = builder.channel;
        this.channelConversationId = builder.channelConversationId;
        this.sourceIdentifier = builder.sourceIdentifier;
        this.sourceName = builder.sourceName;
        this.type = builder.type;
        this.content = builder.content;
        this.mediaUrl = builder.mediaUrl;
        this.caption = builder.caption;
        this.filename = builder.filename;
        this.mimeType = builder.mimeType;
        this.channelMediaId = builder.channelMediaId;
        this.durationSeconds = builder.durationSeconds;
        this.timestamp = builder.timestamp;
        this.tenantId = builder.tenantId;
        this.instanceId = builder.instanceId;
        this.conversationName = builder.conversationName;
        this.metadata = builder.metadata;
        this.outbound = builder.outbound;
        this.group = builder.group;
    }

    public String getChannelMessageId() { return channelMessageId; }
    public ChannelType getChannel() { return channel; }
    public String getChannelConversationId() { return channelConversationId; }
    public String getSourceIdentifier() { return sourceIdentifier; }
    public boolean isInbound() { return !outbound; }
    public String getSourceName() { return sourceName; }
    public MessageType getType() { return type; }
    public String getContent() { return content; }
    public String getMediaUrl() { return mediaUrl; }
    public String getCaption() { return caption; }
    public String getFilename() { return filename; }
    public String getMimeType() { return mimeType; }
    public String getChannelMediaId() { return channelMediaId; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getTenantId() { return tenantId; }
    public String getInstanceId() { return instanceId; }
    public String getConversationName() { return conversationName; }
    public Map<String, Object> getMetadata() { return metadata; }
    public boolean isOutbound() { return outbound; }
    public boolean isGroup() { return group; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String channelMessageId;
        private ChannelType channel;
        private String channelConversationId;
        private String sourceIdentifier;
        private String sourceName;
        private MessageType type = MessageType.TEXT;
        private String content;
        private String mediaUrl;
        private String caption;
        private String filename;
        private String mimeType;
        private String channelMediaId;
        private Integer durationSeconds;
        private LocalDateTime timestamp;
        private String tenantId;
        private String instanceId;
        private String conversationName;
        private Map<String, Object> metadata;
        private boolean outbound;
        private boolean group;

        public Builder channelMessageId(String channelMessageId) { this.channelMessageId = channelMessageId; return this; }
        public Builder channel(ChannelType channel) { this.channel = channel; return this; }
        public Builder channelConversationId(String channelConversationId) { this.channelConversationId = channelConversationId; return this; }
        public Builder sourceIdentifier(String sourceIdentifier) { this.sourceIdentifier = sourceIdentifier; return this; }
        public Builder sourceName(String sourceName) { this.sourceName = sourceName; return this; }
        public Builder type(MessageType type) { this.type = type; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder mediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; return this; }
        public Builder caption(String caption) { this.caption = caption; return this; }
        public Builder filename(String filename) { this.filename = filename; return this; }
        public Builder mimeType(String mimeType) { this.mimeType = mimeType; return this; }
        public Builder channelMediaId(String channelMediaId) { this.channelMediaId = channelMediaId; return this; }
        public Builder durationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; return this; }
        public Builder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder instanceId(String instanceId) { this.instanceId = instanceId; return this; }
        public Builder conversationName(String conversationName) { this.conversationName = conversationName; return this; }
        public Builder metadata(Map<String, Object> metadata) { this.metadata = metadata; return this; }
        public Builder outbound(boolean outbound) { this.outbound = outbound; return this; }
        public Builder group(boolean group) { this.group = group; return this; }

        public IncomingMessage build() {
            return new IncomingMessage(this);
        }
    }
}
