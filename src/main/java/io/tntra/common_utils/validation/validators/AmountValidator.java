package io.tntra.common_utils.validation.validators;

import io.tntra.common_utils.validation.annotations.ValidAmount;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class AmountValidator implements ConstraintValidator<ValidAmount, BigDecimal> {

    private long max;

    @Override
    public void initialize(ValidAmount constraintAnnotation) {
        this.max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        if (value.signum() <= 0) {
            return false;
        }
        BigDecimal maxAmount = BigDecimal.valueOf(max);
        return value.compareTo(maxAmount) <= 0;
    }
}
