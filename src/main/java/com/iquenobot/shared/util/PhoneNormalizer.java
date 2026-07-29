package com.iquenobot.shared.util;

public final class PhoneNormalizer {

    private PhoneNormalizer() {}

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String cleaned = value
                .replaceAll("[^0-9+]", "")
                .trim();

        if (cleaned.isBlank()) {
            return null;
        }

        String digits = cleaned.replaceAll("[^0-9]", "");

        if (digits.matches("^9\\d{8}$")) {
            return "+51" + digits;
        }

        if (digits.matches("^51\\d{9}$")) {
            return "+" + digits;
        }

        if (cleaned.startsWith("+")) {
            return "+" + digits;
        }

        return "+" + digits;
    }
}
