package io.tntra.common_utils.exception.types;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base type for all domain/business/platform exceptions that are exposed
 * to API clients via the standardised {@link io.tntra.common_utils.response.model.ApiResponse}
 * envelope.
 *
 * <h2>Fields</h2>
 * <ul>
 *   <li><b>errorCode</b>  — stable, machine-readable code (e.g. {@code BUSINESS_RULE_VIOLATED}).  
 *       Consumers should switch on this value, not on the human-readable message.</li>
 *   <li><b>message</b>    — human-readable description, propagated via {@link RuntimeException}.</li>
 *   <li><b>httpStatus</b> — HTTP status the {@link io.tntra.common_utils.exception.handler.GlobalExceptionHandler}
 *       will use when building the response.</li>
 * </ul>
 *
 * <h2>PCI/DSS note</h2>
 * Error messages must never contain raw PAN, CVV, or other sensitive card-holder
 * data. Use {@link io.tntra.common_utils.logging.masking.SensitiveDataMasker} before
 * including any field value in the message string.
 */
@Getter
public abstract class BaseException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    /**
     * Primary constructor — use when no underlying cause needs to be preserved.
     *
     * @param errorCode  stable machine-readable code
     * @param message    human-readable description (must not contain PAN/PII)
     * @param httpStatus HTTP status to return to the caller
     */
    protected BaseException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    /**
     * Cause-chaining constructor — use when wrapping a lower-level exception
     * (e.g. {@code SQLException}, {@code IOException}) so that the original
     * stack trace is preserved in the application logs.
     *
     * @param errorCode  stable machine-readable code
     * @param message    human-readable description (must not contain PAN/PII)
     * @param httpStatus HTTP status to return to the caller
     * @param cause      the original exception being wrapped
     */
    protected BaseException(String errorCode, String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
