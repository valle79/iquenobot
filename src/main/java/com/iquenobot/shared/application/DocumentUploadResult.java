package com.iquenobot.shared.application;

/**
 * Resultado de la subida de un documento.
 * Para archivos PDF incluye la extracción de texto automática (solo uso interno
 * del bot); para el resto de documentos pageCount y extractedText son null.
 */
public record DocumentUploadResult(String url, Integer pageCount, String extractedText) {
}
