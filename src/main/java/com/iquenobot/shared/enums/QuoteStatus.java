package com.iquenobot.shared.enums;

public enum QuoteStatus {
    GENERADA,    // Generada por el bot o un usuario
    DRAFT,       // Borrador
    SENT,        // Enviada al cliente
    REENVIADA,   // Reenviada al cliente
    ACCEPTED,    // Aceptada por el cliente
    REJECTED,    // Rechazada
    ANULADA,     // Anulada (baja lógica)
    EXPIRED      // Vencida
}
