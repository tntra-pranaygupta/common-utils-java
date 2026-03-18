package io.tntra.common_utils.exception.handler;

import io.tntra.common_utils.exception.types.BaseException;
import io.tntra.common_utils.exception.types.BusinessException;
import io.tntra.common_utils.exception.types.ValidationException;
import io.tntra.common_utils.logging.masking.SensitiveDataMasker;
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
     * Handles specific domain validation exceptions.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(ValidationException ex) {
        String correlationId = CorrelationIdHolder.getCurrentCorrelationId();
        String maskedMessage = SensitiveDataMasker.maskAll(ex.getMessage());
        log.warn("Validation failure: code={}, message={}, correlationId={}",
                ex.getErrorCode(), maskedMessage, correlationId);
        return responseFactory.error(ex.getErrorCode(), maskedMessage, ex.getHttpStatus().value());
    }

    /**
     * Handles specific business exceptions.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        String correlationId = CorrelationIdHolder.getCurrentCorrelationId();
        String maskedMessage = SensitiveDataMasker.maskAll(ex.getMessage());
        log.warn("Business rule violation: code={}, message={}, correlationId={}",
                ex.getErrorCode(), maskedMessage, correlationId);
        return responseFactory.error(ex.getErrorCode(), maskedMessage, ex.getHttpStatus().value());
    }

    /**
     * Handles all other domain/business/platform exceptions derived from {@link BaseException}.
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException ex) {
        String correlationId = CorrelationIdHolder.getCurrentCorrelationId();
        String maskedMessage = SensitiveDataMasker.maskAll(ex.getMessage());
        log.warn("Domain exception occurred: code={}, message={}, correlationId={}",
                ex.getErrorCode(), maskedMessage, correlationId);
        return responseFactory.error(ex.getErrorCode(), maskedMessage, ex.getHttpStatus().value());
    }

    /**
     * Handles bean validation failures for @Valid request bodies.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String correlationId = CorrelationIdHolder.getCurrentCorrelationId();
        String message = ex.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Validation failed");
        String maskedMessage = SensitiveDataMasker.maskAll(message);
        log.warn("Request validation failed: message={}, correlationId={}", maskedMessage, correlationId);
        return responseFactory.error("VALIDATION_ERROR", maskedMessage, HttpStatus.BAD_REQUEST.value());
    }

    /**
     * Handles constraint violations for @Validated method parameters.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String correlationId = CorrelationIdHolder.getCurrentCorrelationId();
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getMessage())
                .orElse("Validation failed");
        String maskedMessage = SensitiveDataMasker.maskAll(message);
        log.warn("Constraint violation: message={}, correlationId={}", maskedMessage, correlationId);
        return responseFactory.error("VALIDATION_ERROR", maskedMessage, HttpStatus.BAD_REQUEST.value());
    }

    /**
     * Catch-all handler for any unanticipated exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnhandled(Exception ex) {
        String correlationId = CorrelationIdHolder.getCurrentCorrelationId();
        String maskedMessage = SensitiveDataMasker.maskAll(ex.getMessage());
        log.error("Unexpected error: message={}, correlationId={}", maskedMessage, correlationId, ex);
        return responseFactory.error("INTERNAL_ERROR", "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}