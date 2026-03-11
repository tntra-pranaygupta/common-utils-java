package io.tntra.common_utils.exception.types;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a persistence/database level error should be surfaced to clients
 * (after sensitive details have been removed).
 */
public class DatabaseException extends BaseException{
    public DatabaseException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
