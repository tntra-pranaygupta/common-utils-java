package io.tntra.common_utils.exception.types;

import org.springframework.http.HttpStatus;

/**
 * Thrown when input validation fails at the domain layer (beyond simple bean validation).
 *
 * <p>Maps to HTTP 400 Bad Request.</p>
 *
 * <h2>PCI/DSS note</h2>
 * Error messages and codes must not expose PAN, CVV, or card-holder data.
 */
public class ValidationException extends BaseException {

    /**
     * Creates a validation exception with a stable error code and human-readable message.
     *
     * @param errorCode machine-readable identifier (e.g. {@code INVALID_AMOUNT})
     * @param message   human-readable description
     */
    public ValidationException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.BAD_REQUEST);
    }

    /**
     * Cause-chaining variant for wrapping lower-level validation failures.
     *
     * @param errorCode machine-readable identifier
     * @param message   human-readable description
     * @param cause     underlying exception
     */
    public ValidationException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, HttpStatus.BAD_REQUEST, cause);
    }
}
