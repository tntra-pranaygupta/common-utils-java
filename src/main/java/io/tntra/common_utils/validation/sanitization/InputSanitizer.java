package io.tntra.common_utils.validation.sanitization;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * Simple input sanitizer to remove potentially malicious HTML/script tags and common injection patterns.
 * Should be complemented by output encoding at the edge.
 */

@Slf4j
public final class InputSanitizer {

    private static final Pattern SCRIPT_TAG_PATTERN =
            Pattern.compile("(?i)<script.*?>.*?</script>", Pattern.DOTALL);
    private static final Pattern HTML_TAG_PATTERN =
            Pattern.compile("<[^>]+>");
    private static final Pattern SQL_INJECTION_PATTERN =
            Pattern.compile("(?i)(;\\s*drop\\s+table|;\\s*delete\\s+from|--|/\\*|\\*/)");

    private InputSanitizer() {
    }

    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        String original = input;
        String sanitized = SCRIPT_TAG_PATTERN.matcher(input).replaceAll("");
        sanitized = HTML_TAG_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = SQL_INJECTION_PATTERN.matcher(sanitized).replaceAll("");
        if (!original.equals(sanitized)) {
            log.debug("Input sanitized due to potentially malicious content.");
        }
        return sanitized;
    }

}
