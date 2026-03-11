package io.tntra.common_utils.response.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard error object included in API responses when status = "error".
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {

    /**
     * Stable machine-readable error code (e.g. BUSINESS_ERROR, VALIDATION_ERROR).
     */
    private String code;

    /**
     * Human-readable description suitable for clients/logging.
     */
    private String message;
}
