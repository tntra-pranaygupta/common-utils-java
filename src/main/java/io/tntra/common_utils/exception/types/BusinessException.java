package io.tntra.common_utils.exception.types;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a business rule is violated.
 * <p>Maps to HTTP 422 Unprocessable Entity.</p>
 */
public class BusinessException extends BaseException {

    /**
     * Creates a business rule exception.
     *
     * @param errorCode machine-readable identifier (e.g. {@code INSUFFICIENT_FUNDS})
     * @param message   human-readable description
     */
    public BusinessException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Cause-chaining variant.
     *
     * @param errorCode machine-readable identifier
     * @param message   human-readable description
     * @param cause     underlying exception
     */
    public BusinessException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, HttpStatus.UNPROCESSABLE_ENTITY, cause);
    }
}
