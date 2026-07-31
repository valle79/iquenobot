package com.iquenobot.sales.domain.dto;

import com.iquenobot.sales.domain.model.QuoteItemDto;
import com.iquenobot.shared.enums.QuoteStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record QuoteDetailDto(
        UUID id,
        String quoteNumber,
        UUID contactId,
        String customerName,
        String customerPhone,
        String whatsappPhone,
        UUID conversationId,
        LocalDateTime createdAt,
        String currency,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal igv,
        BigDecimal total,
        QuoteStatus status,
        String observations,
        String fileName,
        Long fileSize,
        String fileHash,
        String pdfUrl,
        int resendCount,
        LocalDateTime lastResentAt,
        UUID generatedBy,
        String generatedByName,
        List<QuoteItemDto> items,
        List<QuoteHistoryDto> history) {
}
