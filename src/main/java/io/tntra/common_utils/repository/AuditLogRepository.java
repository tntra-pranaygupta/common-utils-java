package io.tntra.common_utils.repository;

import io.tntra.common_utils.entity.AuditLog;
import io.tntra.common_utils.db_utilities.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends BaseRepository<AuditLog, Long> {
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId);
    List<AuditLog> findByPerformedBy(String performedBy);
}
