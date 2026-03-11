package io.tntra.common_utils.response.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Standard response envelope for all HTTP APIs.
 *
 * {
 *   "status": "success" | "error",
 *   "data": { ... },              // null for errors
 *   "error": { "code": "..", "message": ".." }, // null for successes
 *   "correlation_id": "trace-id"
 * }
 */

@Getter
@Setter(AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private String status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ApiError error;

    @JsonProperty("correlation_id")
    private String correlationId;

    public static <T> ApiResponse<T> success(T data, String correlationId) {
        return ApiResponse.<T>builder()
                .status("success")
                .data(data)
                .error(null)
                .correlationId(correlationId)
                .build();
    }
    public static <T> ApiResponse<T> error(String code, String message, String correlationId) {
        return ApiResponse.<T>builder()
                .status("error")
                .data(null)
                .error(ApiError.builder().code(code).message(message).build())
                .correlationId(correlationId)
                .build();
    }

}
