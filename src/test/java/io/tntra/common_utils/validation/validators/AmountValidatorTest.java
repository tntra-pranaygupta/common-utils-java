package io.tntra.common_utils.validation.validators;

import io.tntra.common_utils.validation.annotations.ValidAmount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AmountValidatorTest {
    private AmountValidator amountValidator;
    @BeforeEach
    void setup() {
        amountValidator = new AmountValidator();
        ValidAmount annotation = mock(ValidAmount.class);
        when(annotation.max()).thenReturn(1000L);
        amountValidator.initialize(annotation);
    }

    /** Validates that positive amounts within the limit are accepted */
    @Test
    void positiveAmountWithinLimitTest() {
        assertThat(amountValidator.isValid(BigDecimal.valueOf(500), null)).isTrue();
    }

    /** Validates that amounts equal to the maximum limit are accepted */
    @Test
    void amountEqualToMaxTest() {
        assertThat(amountValidator.isValid(BigDecimal.valueOf(1000), null)).isTrue();
    }

    /** Validates that zero amounts are rejected */
    @Test
    void zeroAmountTest() {
        assertThat(amountValidator.isValid(BigDecimal.ZERO, null)).isFalse();
    }

    /** Validates that negative amounts are rejected */
    @Test
    void negativeAmountTest() {
        assertThat(amountValidator.isValid(BigDecimal.valueOf(-1), null)).isFalse();
    }

    /** Validates that amounts exceeding the maximum limit are rejected */
    @Test
    void amountExceedingMaxTest() {
        assertThat(amountValidator.isValid(new BigDecimal("1000.01"), null)).isFalse();
    }

    /** Validates that null amounts are rejected */
    @Test
    void nullAmountTest() {
        assertThat(amountValidator.isValid(null, null)).isFalse();
    }
}
