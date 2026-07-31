package com.iquenobot.sales.domain.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record QuoteResendResultDto(
        UUID quoteId,
        String quoteNumber,
        boolean success,
        String message,
        String channel,
        String channelMessageId,
        LocalDateTime resentAt) {
}
