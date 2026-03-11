package io.tntra.common_utils.validation.annotations;

import io.tntra.common_utils.validation.validators.PanValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = PanValidator.class)
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface ValidPan {

    String message() default "Invalid PAN number";

     Class<?>[] groups() default {};

     Class<? extends Payload>[] payload() default {};

}
