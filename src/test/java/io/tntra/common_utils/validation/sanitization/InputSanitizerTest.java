package io.tntra.common_utils.validation.sanitization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InputSanitizerTest {
    /** Validates that sanitize returns null when input is null */
    @Test
    void sanitizeShouldReturnNullWhenInputNullTest() {
        assertThat(InputSanitizer.sanitize(null)).isNull();
    }

    /** Validates that sanitize removes script tags */
    @Test
    void sanitizeShouldRemoveScriptTagsTest() {
        String input = "hello<script>alert('xss');</script>world";
        assertThat(InputSanitizer.sanitize(input)).isEqualTo("helloworld");
    }

    /** Validates that sanitize removes HTML tags */
    @Test
    void sanitizeShouldRemoveHtmlTagsTest() {
        String input = "<b>bold</b><div>text</div>";
        assertThat(InputSanitizer.sanitize(input)).isEqualTo("boldtext");
    }

    /** Validates that sanitize removes common injection tokens */
    @Test
    void sanitizeShouldRemoveCommonInjectionTokensTest() {
        String input = "abc; DROP TABLE users --";
        String sanitized = InputSanitizer.sanitize(input);
        assertThat(sanitized).doesNotContain("DROP TABLE");
        assertThat(sanitized).doesNotContain("--");
    }

    /** Validates that sanitize returns the same string when no patterns are matched */
    @Test
    void sanitizeShouldReturnSameStringWhenNoPatternsMatchedTest() {
        String input = "simple-text_123";
        assertThat(InputSanitizer.sanitize(input)).isEqualTo(input);
    }
}
