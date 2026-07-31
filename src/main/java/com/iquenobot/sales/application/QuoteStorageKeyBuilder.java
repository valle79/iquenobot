package com.iquenobot.sales.application;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * Builds structured object keys for quote PDFs:
 * {tenantId}/cotizaciones/yyyy/MM/COT-yyyyMMdd-0000001.pdf
 */
public final class QuoteStorageKeyBuilder {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT);

    private QuoteStorageKeyBuilder() {}

    public static String build(String quoteNumber, UUID tenantId, LocalDateTime createdAt) {
        String sequence = quoteNumber != null && quoteNumber.contains("-")
                ? quoteNumber.substring(quoteNumber.indexOf('-') + 1)
                : quoteNumber != null ? quoteNumber : "0000000";
        LocalDateTime date = createdAt != null ? createdAt : LocalDateTime.now();
        String year = String.valueOf(date.getYear());
        String month = String.format(Locale.ROOT, "%02d", date.getMonthValue());
        return tenantId + "/cotizaciones/" + year + "/" + month + "/COT-" + date.format(FILE_DATE) + "-" + sequence + ".pdf";
    }
}
