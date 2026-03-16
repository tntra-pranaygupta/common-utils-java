package io.tntra.common_utils.exception.types;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a persistence/database level error should be surfaced to clients
 * (after sensitive details have been removed).
 * <p>Maps to HTTP 500 Internal Server Error.</p>
 */
public class DatabaseException extends BaseException {

    /**
     * Creates a database exception.
     *
     * @param errorCode machine-readable identifier
     * @param message   human-readable description
     */
    public DatabaseException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Cause-chaining variant.
     *
     * @param errorCode machine-readable identifier
     * @param message   human-readable description
     * @param cause     underlying database exception (e.g., SQLException, DataAccessException)
     */
    public DatabaseException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
