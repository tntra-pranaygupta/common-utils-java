package io.tntra.common_utils.exception.handler;

import io.tntra.common_utils.exception.types.BaseException;
import io.tntra.common_utils.response.factory.ResponseFactory;
import io.tntra.common_utils.response.model.ApiResponse;
import io.tntra.common_utils.util.CorrelationIdHolder;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler that guarantees all errors are returned using the
 * standardized {@link ApiResponse} envelope.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ResponseFactory responseFactory;

    public GlobalExceptionHandler(ResponseFactory responseFactory) {
        this.responseFactory = responseFactory;
    }

    /**
     * Handles all domain/business/platform exceptions derived from {@link BaseException}.
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException ex) {
        log.warn("Domain exception occurred code={}, message={}", ex.getErrorCode(), ex.getMessage());
        return responseFactory.error(ex.getErrorCode(), ex.getMessage(), ex.getHttpStatus().value());
    }

    /**
     * Handles bean validation failures for @Valid request bodies.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Validation failed");
        log.warn("Request validation failed: {}", message);
        return responseFactory.error("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST.value());
    }

    /**
     * Handles constraint violations for @Validated method parameters.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getMessage())
                .orElse("Validation failed");
        log.warn("Constraint violation: {}", message);
        return responseFactory.error("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST.value());
    }

    /**
     * Catch-all handler for any unanticipated exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnhandled(Exception ex) {
        String correlationId = CorrelationIdHolder.getCurrentCorrelationId();
        log.error("Unexpected error, correlationId={}", correlationId, ex);
        return responseFactory.error("INTERNAL_ERROR", "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
