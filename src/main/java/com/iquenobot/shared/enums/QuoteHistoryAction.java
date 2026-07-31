package com.iquenobot.shared.enums;

/**
 * Actions tracked in the quote history (audit trail of each quote).
 */
public enum QuoteHistoryAction {
    GENERATED,    // Generación de la cotización
    SENT,         // Envío inicial al cliente
    RESENT,       // Reenvío del PDF al cliente
    REGENERATED,  // PDF regenerado
    DOWNLOADED,   // PDF descargado desde el módulo
    CANCELLED,    // Cotización anulada
    ERROR         // Error en generación / envío / reenvío
}
