package io.tntra.common_utils.validation.validators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PanValidatorTest {

    private PanValidator panValidator;

    @BeforeEach
    void setup() {
        panValidator = new PanValidator();
    }

    /** Validates that a valid PAN passes Luhn algorithm check */
    @Test
    void validPanTest() {
        assertThat(panValidator.isValid("4111111111111111", null)).isTrue();
        assertThat(panValidator.isValid("4532015112830366", null)).isTrue();

    }

    /** Validates that an invalid PAN fails Luhn algorithm check */
    @Test
    void invalidPanTest() {
        assertThat(panValidator.isValid("4111111111111112", null)).isFalse();
    }

    /** Validates that non-numeric PAN is rejected */
    @Test
    void nonNumericPanTest() {
        assertThat(panValidator.isValid("ABCDEF1234567890", null)).isFalse();
    }

    /** Validates that blank PAN is rejected */
    @Test
    void blankPanTest() {
        assertThat(panValidator.isValid(" ", null)).isFalse();
    }

    /** Validates that null PAN is rejected */
    @Test
    void nullPanTest() {
        assertThat(panValidator.isValid(null, null)).isFalse();
    }
}