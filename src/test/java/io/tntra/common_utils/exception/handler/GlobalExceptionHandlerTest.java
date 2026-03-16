package io.tntra.common_utils.exception.handler;

import io.tntra.common_utils.exception.types.BaseException;
import io.tntra.common_utils.exception.types.BusinessException;
import io.tntra.common_utils.exception.types.ValidationException;
import io.tntra.common_utils.response.factory.ResponseFactory;
import io.tntra.common_utils.response.model.ApiResponse;
import io.tntra.common_utils.util.CorrelationIdHolder;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setup() {
        handler = new GlobalExceptionHandler(new ResponseFactory());
        CorrelationIdHolder.setCurrentCorrelationId("corr-test");
    }

    @AfterEach
    void tearDown() {
        CorrelationIdHolder.setCurrentCorrelationId(null);
    }

    /**
     * Should return a standard error envelope for BaseException.
     */
    @Test
    void handleBaseExceptionShouldReturnStandardErrorEnvelopeTest() {
        BaseException ex = new BaseException("GENERIC_ERROR", "A domain error like card 4111-2222-3333-4444", HttpStatus.BAD_REQUEST) {};

        ResponseEntity<ApiResponse<Void>> response = handler.handleBaseException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(ex.getHttpStatus().value());
        assertThat(response.getBody()).isNotNull();
        ApiResponse<Void> body = response.getBody();
        assertThat(body.getStatus()).isEqualTo("error");
        assertThat(body.getError().getCode()).isEqualTo("GENERIC_ERROR");
        assertThat(body.getError().getMessage()).isEqualTo("A domain error like card ****-****-****-4444");
        assertThat(body.getCorrelationId()).isEqualTo("corr-test");
    }

    /**
     * Should return a standard error envelope for BusinessException.
     */
    @Test
    void handleBusinessExceptionShouldReturnStandardErrorEnvelopeTest() {
        BusinessException ex = new BusinessException("BUSINESS_ERROR", "Business rule violated for test@example.com");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(ex.getHttpStatus().value());
        assertThat(response.getBody()).isNotNull();
        ApiResponse<Void> body = response.getBody();
        assertThat(body.getStatus()).isEqualTo("error");
        assertThat(body.getError().getCode()).isEqualTo("BUSINESS_ERROR");
        assertThat(body.getError().getMessage()).isEqualTo("Business rule violated for ****@example.com");
        assertThat(body.getCorrelationId()).isEqualTo("corr-test");
    }
    
    /**
     * Should return a standard error envelope for ValidationException.
     */
    @Test
    void handleValidationExceptionShouldReturnStandardErrorEnvelopeTest() {
        ValidationException ex = new ValidationException("VALIDATION_ERROR", "validation violated for 4111222233334444");

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(ex.getHttpStatus().value());
        assertThat(response.getBody()).isNotNull();
        ApiResponse<Void> body = response.getBody();
        assertThat(body.getStatus()).isEqualTo("error");
        assertThat(body.getError().getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.getError().getMessage()).isEqualTo("validation violated for ****-****-****-4444");
        assertThat(body.getCorrelationId()).isEqualTo("corr-test");
    }

    /**
     * Should return a validation error envelope for MethodArgumentNotValidException.
     */
    @Test
    void handleMethodArgumentNotValidShouldReturnValidationErrorTest() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "objectName");
        bindingResult.addError(new FieldError("objectName", "field", "Field is required for john.doe@mail.com."));

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiResponse<Void>> response = handler.handleMethodArgumentNotValid(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);

        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo("error");
        assertThat(body.getError()).isNotNull();
        assertThat(body.getError().getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.getError().getMessage()).isEqualTo("Field is required for ****@mail.com.");
        assertThat(body.getCorrelationId()).isEqualTo("corr-test");
    }

    /**
     * Should return a validation error envelope for ConstraintViolationException.
     */
    @Test
    void handleConstraintViolationShouldReturnValidationErrorTest() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("Parameter invalid max 4111-2222-3333-4444 length");

        ConstraintViolationException ex =
                new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ApiResponse<Void>> response = handler.handleConstraintViolation(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);

        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo("error");
        assertThat(body.getError().getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.getError().getMessage()).isEqualTo("Parameter invalid max ****-****-****-4444 length");
        assertThat(body.getCorrelationId()).isEqualTo("corr-test");
    }

    /**
     * Should return an internal server error envelope for unhandled exceptions.
     */
    @Test
    void handleUnhandledShouldReturnInternalServerErrorTest() {
        Exception ex = new RuntimeException("Unexpected failure 4111222233334444");

        ResponseEntity<ApiResponse<Void>> response = handler.handleUnhandled(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(500);

        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo("error");
        assertThat(body.getError().getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(body.getError().getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(body.getCorrelationId()).isEqualTo("corr-test");
    }
}
