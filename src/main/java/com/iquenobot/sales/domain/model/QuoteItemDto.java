package com.iquenobot.sales.domain.model;

import java.math.BigDecimal;

public record QuoteItemDto(String name, String sku, int quantity, BigDecimal unitPrice) {

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
