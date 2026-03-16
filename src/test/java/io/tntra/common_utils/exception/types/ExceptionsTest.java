package io.tntra.common_utils.exception.types;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionsTest {

    /**
     * Should instantiate BusinessException with correct properties.
     */
    @Test
    void businessExceptionShouldInstantiateProperlyTest() {
        BusinessException ex = new BusinessException("LOCKED_ACCOUNT", "Account is locked.");
        
        assertThat(ex.getErrorCode()).isEqualTo("LOCKED_ACCOUNT");
        assertThat(ex.getMessage()).isEqualTo("Account is locked.");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ex.getCause()).isNull();
    }

    /**
     * Should preserve original exception as cause in BusinessException.
     */
    @Test
    void businessExceptionWithCauseShouldPreserveOriginalExceptionTest() {
        Throwable cause = new IllegalArgumentException("Sub-system failure");
        BusinessException ex = new BusinessException("LOCKED_ACCOUNT", "Account is locked.", cause);
        
        assertThat(ex.getCause()).isEqualTo(cause);
    }
    
    /**
     * Should instantiate ValidationException with correct properties.
     */
    @Test
    void validationExceptionShouldInstantiateProperlyTest() {
        ValidationException ex = new ValidationException("INVALID_EMAIL", "Email must be standard string.");
        
        assertThat(ex.getErrorCode()).isEqualTo("INVALID_EMAIL");
        assertThat(ex.getMessage()).isEqualTo("Email must be standard string.");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getCause()).isNull();
    }

    /**
     * Should preserve original exception as cause in ValidationException.
     */
    @Test
    void validationExceptionWithCauseShouldPreserveOriginalExceptionTest() {
        Throwable cause = new NumberFormatException("Not a number.");
        ValidationException ex = new ValidationException("INVALID_EMAIL", "Email must be standard string.", cause);
        
        assertThat(ex.getCause()).isEqualTo(cause);
    }
    
    /**
     * Should instantiate DatabaseException with correct properties.
     */
    @Test
    void databaseExceptionShouldInstantiateProperlyTest() {
        DatabaseException ex = new DatabaseException("CONN_FAILURE", "Failed getting connection.");
        
        assertThat(ex.getErrorCode()).isEqualTo("CONN_FAILURE");
        assertThat(ex.getMessage()).isEqualTo("Failed getting connection.");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(ex.getCause()).isNull();
    }

    /**
     * Should preserve original exception as cause in DatabaseException.
     */
    @Test
    void databaseExceptionWithCauseShouldPreserveOriginalExceptionTest() {
        Throwable cause = new RuntimeException("DB driver error");
        DatabaseException ex = new DatabaseException("CONN_FAILURE", "Failed getting connection.", cause);
        
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
