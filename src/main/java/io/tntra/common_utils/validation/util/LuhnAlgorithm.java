package io.tntra.common_utils.validation.util;

/**
 * Utility implementing the Luhn algorithm for PAN validation.
 */
public final class LuhnAlgorithm {

    private LuhnAlgorithm() {
    }

    public static boolean isValid(String pan) {
        if(pan == null || pan.isEmpty()) {
            return false;
        }
        if (!pan.chars().allMatch(Character::isDigit)) {
            return false;
        }
        int sum = 0;
        boolean alternate = false;
        for (int i = pan.length() - 1; i >= 0; i--) {
            int n = pan.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    /**
     * Masks a PAN value for logging, preserving only the last 4 digits.
     */
    public static String maskPan(String pan) {
        if (pan == null || pan.length() < 4) {
            return "****";
        }
        String last4 = pan.substring(pan.length() - 4);
        return "****-****-****-" + last4;
    }
}
