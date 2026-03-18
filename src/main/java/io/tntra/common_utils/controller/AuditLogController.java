package io.tntra.common_utils.controller;

import io.tntra.common_utils.dto.AuditLogResponseDto;
import io.tntra.common_utils.service.AuditLogService;
import io.tntra.common_utils.response.factory.ResponseFactory;
import io.tntra.common_utils.response.model.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final ResponseFactory responseFactory;

    public AuditLogController(AuditLogService auditLogService, ResponseFactory responseFactory) {
        this.auditLogService = auditLogService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AuditLogResponseDto>>> getLogsByEntity(
            @RequestParam String entityType,
            @RequestParam String entityId) {
        List<AuditLogResponseDto> response = auditLogService.getLogsByEntity(entityType, entityId);
        return responseFactory.success(response);
    }

    @GetMapping("/by-user")
    public ResponseEntity<ApiResponse<List<AuditLogResponseDto>>> getLogsByUser(
            @RequestParam String performedBy) {
        List<AuditLogResponseDto> response = auditLogService.getLogsByPerformedBy(performedBy);
        return responseFactory.success(response);
    }
}
