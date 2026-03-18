package io.tntra.common_utils.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AuditLogResponseDto {
    private Long id;
    private String entityType;
    private String entityId;
    private String action;
    private String performedBy;
    private Instant performedAt;
    private String changeSummary;
    private String correlationId;
}
