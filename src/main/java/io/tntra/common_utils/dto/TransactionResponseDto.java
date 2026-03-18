package io.tntra.common_utils.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class TransactionResponseDto {
    private String externalId;
    private String cardScheme;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String failureReason;
    private String correlationId;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
}
