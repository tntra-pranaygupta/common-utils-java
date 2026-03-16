package io.tntra.common_utils.validation.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LuhnAlgorithmTest {

    /** Validates that null or empty input returns false for Luhn validation */
    @Test
    void isValidShouldReturnFalseForNullOrEmptyTest() {
        assertThat(LuhnAlgorithm.isValid(null)).isFalse();
        assertThat(LuhnAlgorithm.isValid("")).isFalse();
    }

    /** Validates that PAN masking works correctly for various inputs */
    @Test
    void maskPanShouldMaskCorrectlyTest() {
        assertThat(LuhnAlgorithm.maskPan(null)).isEqualTo("****");
        assertThat(LuhnAlgorithm.maskPan("1")).isEqualTo("****");
        assertThat(LuhnAlgorithm.maskPan("1234")).isEqualTo("****-****-****-1234");
        assertThat(LuhnAlgorithm.maskPan("4111111111111111")).isEqualTo("****-****-****-1111");
    }
}
