package io.tntra.common_utils.exception.handler;

import io.tntra.common_utils.exception.types.BusinessException;
import io.tntra.common_utils.response.factory.ResponseFactory;
import io.tntra.common_utils.response.model.ApiResponse;
import io.tntra.common_utils.util.CorrelationIdHolder;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GlobalExceptionHandlerTest {
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

    /** Validates that base exceptions are handled and return standard error envelope */
    @Test
    void handleBaseExceptionShouldReturnStandardErrorEnvelopeTest() {
        BusinessException ex = new BusinessException("BUSINESS_ERROR", "Business rule violated");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBaseException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(ex.getHttpStatus().value());
        assertThat(response.getBody()).isNotNull();
        ApiResponse<Void> body = response.getBody();
        assertThat(body.getStatus()).isEqualTo("error");
        assertThat(body.getData()).isNull();
        assertThat(body.getError()).isNotNull();
        assertThat(body.getError().getCode()).isEqualTo("BUSINESS_ERROR");
        assertThat(body.getCorrelationId()).isEqualTo("corr-test");
    }


    /** Validates that @Valid request body validation errors are handled */
    @Test
    void handleMethodArgumentNotValidShouldReturnValidationErrorTest() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "objectName");
        bindingResult.addError(new FieldError("objectName", "field", "Field is required"));

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiResponse<Void>> response = handler.handleMethodArgumentNotValid(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);

        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo("error");
        assertThat(body.getError()).isNotNull();
        assertThat(body.getError().getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.getError().getMessage()).isEqualTo("Field is required");
        assertThat(body.getCorrelationId()).isEqualTo("corr-test");
    }


    /** Validates that @Validated parameter constraint violations are handled */
    @Test
    void handleConstraintViolationShouldReturnValidationErrorTest() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("Parameter invalid");

        ConstraintViolationException ex =
                new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ApiResponse<Void>> response = handler.handleConstraintViolation(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);

        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo("error");
        assertThat(body.getError().getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.getError().getMessage()).isEqualTo("Parameter invalid");
        assertThat(body.getCorrelationId()).isEqualTo("corr-test");
    }


    /** Validates that unexpected exceptions return internal server error */
    @Test
    void handleUnhandledShouldReturnInternalServerErrorTest() {
        Exception ex = new RuntimeException("Unexpected failure");

        ResponseEntity<ApiResponse<Void>> response = handler.handleUnhandled(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(500);

        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo("error");
        assertThat(body.getError()).isNotNull();
        assertThat(body.getError().getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(body.getCorrelationId()).isEqualTo("corr-test");
    }
}
