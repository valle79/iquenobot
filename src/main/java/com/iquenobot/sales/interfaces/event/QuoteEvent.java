package com.iquenobot.sales.interfaces.event;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Domain event emitted for every relevant quote lifecycle action
 * (generation, send, resend, regeneration, download, cancellation, error).
 */
public class QuoteEvent {

    private final UUID eventId;
    private final LocalDateTime occurredAt;
    private final QuoteEventType type;
    private final UUID tenantId;
    private final UUID quoteId;
    private final String quoteNumber;
    private final UUID actorId;
    private final String actorName;
    private final String channel;
    private final String channelMessageId;
    private final String details;

    public QuoteEvent(QuoteEventType type, UUID tenantId, UUID quoteId, String quoteNumber,
                      UUID actorId, String actorName, String channel, String channelMessageId, String details) {
        this.eventId = UUID.randomUUID();
        this.occurredAt = LocalDateTime.now(ZoneOffset.UTC);
        this.type = type;
        this.tenantId = tenantId;
        this.quoteId = quoteId;
        this.quoteNumber = quoteNumber;
        this.actorId = actorId;
        this.actorName = actorName;
        this.channel = channel;
        this.channelMessageId = channelMessageId;
        this.details = details;
    }

    public UUID getEventId() { return eventId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public QuoteEventType getType() { return type; }
    public UUID getTenantId() { return tenantId; }
    public UUID getQuoteId() { return quoteId; }
    public String getQuoteNumber() { return quoteNumber; }
    public UUID getActorId() { return actorId; }
    public String getActorName() { return actorName; }
    public String getChannel() { return channel; }
    public String getChannelMessageId() { return channelMessageId; }
    public String getDetails() { return details; }
}
