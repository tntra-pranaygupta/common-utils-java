package io.tntra.common_utils.validation.validators;

import io.tntra.common_utils.validation.annotations.ValidPan;
import io.tntra.common_utils.validation.util.LuhnAlgorithm;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PanValidator implements ConstraintValidator<ValidPan, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if(value == null || value.isBlank()) {
            return false;
        }
        boolean valid = LuhnAlgorithm.isValid(value);
        if(!valid){
            log.warn("PAN validation failed for masked value: {}", LuhnAlgorithm.maskPan(value));
        }
        return valid;
    }
}
