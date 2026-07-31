package com.iquenobot.sales.domain.dto;

import com.iquenobot.shared.enums.QuoteHistoryAction;

import java.time.LocalDateTime;
import java.util.UUID;

public record QuoteHistoryDto(
        UUID id,
        QuoteHistoryAction action,
        String actorName,
        String channel,
        String channelMessageId,
        String details,
        LocalDateTime createdAt) {
}
