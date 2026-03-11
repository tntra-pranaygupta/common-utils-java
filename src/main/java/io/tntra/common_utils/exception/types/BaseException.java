package io.tntra.common_utils.exception.types;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base type for all domain/business/platform exceptions that should be exposed
 * to API clients via the standardized response envelope.
 */
@Getter
public abstract class BaseException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    protected BaseException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
