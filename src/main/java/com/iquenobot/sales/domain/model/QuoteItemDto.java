package com.iquenobot.sales.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Ítem de cotización. El subtotal se calcula (getSubtotal) y por tanto se
 * serializa en el JSON persistido; al deserializar se ignora ese campo para
 * recalcularlo, manteniendo compatibilidad con cotizaciones existentes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record QuoteItemDto(String name, String sku, int quantity, BigDecimal unitPrice) {

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
