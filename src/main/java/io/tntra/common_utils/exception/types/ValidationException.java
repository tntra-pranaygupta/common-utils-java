package io.tntra.common_utils.exception.types;

import org.springframework.http.HttpStatus;

/**
 * Thrown when input validation fails at the domain layer (beyond simple bean validation).
 */
public class ValidationException extends BaseException {
    public ValidationException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.BAD_REQUEST);
    }
}
