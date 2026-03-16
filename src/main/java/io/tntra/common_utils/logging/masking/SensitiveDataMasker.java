package io.tntra.common_utils.logging.masking;

import java.util.regex.Pattern;

/**
 * Utility for masking sensitive data (PAN, PII) in log messages.
 *
 * <h2>PCI/DSS Requirements</h2>
 * <ul>
 *   <li>PAN values must NEVER appear in logs in plain text (PCI DSS Requirement 3.4).</li>
 *   <li>Only the last 4 digits of a PAN may be displayed (PCI DSS Req 3.3).</li>
 *   <li>This class provides both explicit masking methods and regex-based
 *       auto-detection for strings that may inadvertently embed card numbers.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 *   // Explicit PAN masking before logging
 *   log.warn("PAN validation failed for card: {}", SensitiveDataMasker.maskPan(pan));
 *
 *   // Mask any PAN-like sequences embedded in arbitrary text
 *   log.info("Request payload: {}", SensitiveDataMasker.maskEmbeddedPans(rawPayload));
 * }</pre>
 */
public final class SensitiveDataMasker {

    /**
     * Matches 13–19 consecutive digits that may represent a PAN.
     * Uses a word-boundary assertion to avoid matching partial numbers.
     */
    private static final Pattern PAN_PATTERN =
            Pattern.compile("\\b(\\d{4})[\\s\\-]?(\\d{4})[\\s\\-]?(\\d{4})[\\s\\-]?(\\d{1,7})\\b");

    /**
     * Generic email pattern for masking PII in logs.
     * Preserves the domain so log correlation remains possible.
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("([a-zA-Z0-9._%+\\-]+)(@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,})");

    /**
     * Mask characters used for PAN replacement: show only last 4 digits.
     */
    private static final String PAN_MASK_PREFIX = "****-****-****-";

    private SensitiveDataMasker() {
        // utility class – not instantiable
    }

    // ─── PAN masking ──────────────────────────────────────────────────────────

    /**
     * Masks a standalone PAN value, preserving only the last 4 digits.
     *
     * <p>Returns {@code "****"} for null or short inputs to prevent NullPointerException
     * in log arguments.</p>
     *
     * @param pan raw PAN string (may be null)
     * @return masked string safe for logging
     */
    public static String maskPan(String pan) {
        if (pan == null || pan.length() < 4) {
            return "****";
        }
        // Strip spaces/dashes before extracting last 4
        String digits = pan.replaceAll("[\\s\\-]", "");
        if (digits.length() < 4) {
            return "****";
        }
        return PAN_MASK_PREFIX + digits.substring(digits.length() - 4);
    }

    /**
     * Scans arbitrary text and replaces any PAN-like digit sequences with a
     * masked representation. This provides a defensive safety net when raw
     * request payloads must be logged for debugging.
     *
     * @param text text that may contain embedded PANs (may be null)
     * @return text with all PAN-like sequences replaced, or null if input was null
     */
    public static String maskEmbeddedPans(String text) {
        if (text == null) {
            return null;
        }
        return PAN_PATTERN.matcher(text).replaceAll(m -> {
            // Reconstruct masked: keep last group (last 1-7 digits), mask rest
            String lastGroup = m.group(4);
            // Pad last group to 4 chars for uniform display
            String last4 = lastGroup.length() >= 4
                    ? lastGroup.substring(lastGroup.length() - 4)
                    : String.format("%4s", lastGroup).replace(' ', '*');
            return PAN_MASK_PREFIX + last4;
        });
    }

    // ─── PII masking ──────────────────────────────────────────────────────────

    /**
     * Masks an email address, preserving only the domain portion.
     *
     * <p>Example: {@code john.doe@example.com} → {@code ****@example.com}</p>
     *
     * @param email raw email (may be null)
     * @return masked email safe for logging
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "****";
        }
        return EMAIL_PATTERN.matcher(email).replaceAll("****$2");
    }

    /**
     * Masks any email addresses embedded within arbitrary text.
     *
     * @param text text that may contain email addresses (may be null)
     * @return text with all email addresses masked
     */
    public static String maskEmbeddedEmails(String text) {
        if (text == null) {
            return null;
        }
        return EMAIL_PATTERN.matcher(text).replaceAll("****$2");
    }

    /**
     * Applies all masks (PAN + email) to arbitrary text in a single pass.
     * Use this when you want full sanitisation of a log message payload.
     *
     * @param text arbitrary text (may be null)
     * @return fully masked text
     */
    public static String maskAll(String text) {
        if (text == null) {
            return null;
        }
        return maskEmbeddedEmails(maskEmbeddedPans(text));
    }
}
