package io.tntra.common_utils.audit.service;

import io.tntra.common_utils.audit.dto.AuditLogResponseDto;
import io.tntra.common_utils.audit.entity.AuditLog;
import io.tntra.common_utils.audit.repository.AuditLogRepository;
import io.tntra.common_utils.util.CorrelationIdHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponseDto> getLogsByEntity(String entityType, String entityId) {
        if (entityType == null || entityType.trim().isEmpty()) {
            throw new io.tntra.common_utils.exception.types.ValidationException("INVALID_INPUT", "entityType cannot be null or empty");
        }
        if (entityId == null || entityId.trim().isEmpty()) {
            throw new io.tntra.common_utils.exception.types.ValidationException("INVALID_INPUT", "entityId cannot be null or empty");
        }
        
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponseDto> getLogsByPerformedBy(String performedBy) {
        if (performedBy == null || performedBy.trim().isEmpty()) {
            throw new io.tntra.common_utils.exception.types.ValidationException("INVALID_INPUT", "performedBy cannot be null or empty");
        }
    
        return auditLogRepository.findByPerformedBy(performedBy).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void logAction(String entityType, String entityId, String action, String changeSummary) {
        AuditLog log = new AuditLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setChangeSummary(changeSummary);
        log.setPerformedAt(Instant.now());
        log.setCorrelationId(CorrelationIdHolder.getCurrentCorrelationId());
        
        // Use the existing DefaultAuditorAware to get the current auditor
        String performedBy = new io.tntra.common_utils.db.auditing.DefaultAuditorAware()
                .getCurrentAuditor()
                .orElse("system");
        
        log.setPerformedBy(performedBy);
        
        auditLogRepository.save(log);
    }

    private AuditLogResponseDto mapToDto(AuditLog entity) {
        return AuditLogResponseDto.builder()
                .id(entity.getId())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .action(entity.getAction())
                .performedBy(entity.getPerformedBy())
                .performedAt(entity.getPerformedAt())
                .changeSummary(entity.getChangeSummary())
                .correlationId(entity.getCorrelationId())
                .build();
    }
}
