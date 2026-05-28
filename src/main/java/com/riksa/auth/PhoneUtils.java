package com.riksa.auth;

public final class PhoneUtils {

    private PhoneUtils() {}

    /**
     * Normalize a Cambodian phone number to international format (+855XXXXXXXXX).
     * Accepts: 0XXXXXXXX(X), +855XXXXXXXX(X), 855XXXXXXXX(X)
     */
    public static String normalize(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return null;
        }

        String digits = phoneNumber.replaceAll("[^0-9+]", "");

        if (digits.startsWith("+855")) {
            return digits;
        }

        if (digits.startsWith("855")) {
            return "+" + digits;
        }

        if (digits.startsWith("0")) {
            return "+855" + digits.substring(1);
        }

        return digits;
    }

    /**
     * Validate that a phone number is a valid Cambodian number.
     */
    public static boolean isValidCambodianNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return false;
        }

        String normalized = normalize(phoneNumber);
        if (normalized == null) {
            return false;
        }

        String nationalNumber = normalized.substring(4);
        return nationalNumber.matches("\\d{8,9}");
    }
}
