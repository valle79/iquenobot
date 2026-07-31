package com.iquenobot.sales.domain.dto;

import com.iquenobot.shared.enums.QuoteStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record QuoteSummaryDto(
        UUID id,
        String quoteNumber,
        String customerName,
        String customerPhone,
        String whatsappPhone,
        LocalDateTime createdAt,
        BigDecimal total,
        String currency,
        QuoteStatus status,
        int resendCount,
        LocalDateTime lastResentAt,
        UUID generatedBy,
        String generatedByName,
        String pdfUrl,
        String fileName,
        Long fileSize) {
}
