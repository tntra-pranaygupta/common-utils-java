package io.tntra.common_utils.audit.service;

import io.tntra.common_utils.audit.dto.AuditLogResponseDto;

import java.util.List;

public interface AuditLogService {
    List<AuditLogResponseDto> getLogsByEntity(String entityType, String entityId);
    List<AuditLogResponseDto> getLogsByPerformedBy(String performedBy);
    void logAction(String entityType, String entityId, String action, String changeSummary);
}
