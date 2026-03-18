package io.tntra.common_utils.response.factory;

import io.tntra.common_utils.response.model.ApiError;
import io.tntra.common_utils.response.model.ApiResponse;
import io.tntra.common_utils.util.CorrelationIdHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Factory for building standardized API responses with the current correlation ID.
 *
 * Intended usage from controllers:
 *   return responseFactory.success(dto);
 *   // or
 *   return responseFactory.error("BUSINESS_ERROR", "Something failed", HttpStatus.UNPROCESSABLE_ENTITY.value());
 */

@Component
public class ResponseFactory {

    public <T> ResponseEntity<ApiResponse<T>> success(T body) {
        String correlationId = CorrelationIdHolder.getCurrentCorrelationId();
        return ResponseEntity.ok(ApiResponse.success(body, correlationId));
    }
    public <T> ResponseEntity<ApiResponse<T>> created(T body) {
        String correlationId = CorrelationIdHolder.getCurrentCorrelationId();
        return ResponseEntity.status(201).body(ApiResponse.success(body, correlationId));
    }
    public <T> ResponseEntity<ApiResponse<T>> error(String code, String message, int httpStatus) {
        String correlationId = CorrelationIdHolder.getCurrentCorrelationId();
        return ResponseEntity.status(httpStatus)
                .body(ApiResponse.error(code, message, correlationId));
    }
}
