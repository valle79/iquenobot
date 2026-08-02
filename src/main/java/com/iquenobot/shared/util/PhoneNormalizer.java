
package com.iquenobot.shared.util;

public final class PhoneNormalizer {

    private PhoneNormalizer() {
    }

    public static String normalize(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        String cleaned = value.trim();

        // Eliminar sufijos de WhatsApp
        cleaned = cleaned.replace("@s.whatsapp.net", "")
                         .replace("@g.us", "")
                         .replace("@lid", "");

        // Conservar solo dígitos y +
        cleaned = cleaned.replaceAll("[^0-9+]", "");

        if (cleaned.isBlank()) {
            return null;
        }

        String digits = cleaned.replaceAll("\\D", "");

        // Perú: 9XXXXXXXX
        if (digits.matches("^9\\d{8}$")) {
            return "+51" + digits;
        }

        // Perú: 51XXXXXXXXX
        if (digits.matches("^51\\d{9}$")) {
            return "+" + digits;
        }

        // Internacional
        if (digits.length() >= 9 && digits.length() <= 15) {
            return "+" + digits;
        }

        return null;
    }
}
