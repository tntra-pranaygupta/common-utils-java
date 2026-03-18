package io.tntra.common_utils.service.impl;

import io.tntra.common_utils.dto.AuditLogResponseDto;
import io.tntra.common_utils.entity.AuditLog;
import io.tntra.common_utils.repository.AuditLogRepository;
import io.tntra.common_utils.exception.types.ValidationException;
import io.tntra.common_utils.util.CorrelationIdHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditLogServiceImpl with 100% code coverage.
 *
 * Tests cover:
 * - getLogsByEntity: happy path, validation (null/empty entityType and entityId)
 * - getLogsByPerformedBy: happy path, validation (null/empty performedBy)
 * - logAction: with correlation ID, without correlation ID, auditor resolution
 * - mapToDto: all fields mapping
 * - Edge cases: empty results, multiple results, various field values
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogServiceImpl auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogServiceImpl(auditLogRepository);
        MDC.clear();
    }

    @Nested
    @DisplayName("getLogsByEntity Tests")
    class GetLogsByEntityTests {

        /**
         * Test successful retrieval of logs by entity type and ID.
         * Verifies: logs are found and mapped correctly.
         */
        @Test
        void testGetLogsByEntitySuccessfullyTest() {
            String entityType = "FinancialTransaction";
            String entityId = "txn-123";

            List<AuditLog> logs = Arrays.asList(
                    buildAuditLog(1L, entityType, entityId, "CREATE", "system", "Created transaction"),
                    buildAuditLog(2L, entityType, entityId, "UPDATE_STATUS", "admin", "Status changed")
            );

            when(auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId))
                    .thenReturn(logs);
            List<AuditLogResponseDto> result = auditLogService.getLogsByEntity(entityType, entityId);
            assertThat(result)
                    .isNotNull()
                    .hasSize(2)
                    .extracting("entityType", "entityId", "action")
                    .containsExactly(
                            tuple(entityType, entityId, "CREATE"),
                            tuple(entityType, entityId, "UPDATE_STATUS")
                    );

            verify(auditLogRepository, times(1))
                    .findByEntityTypeAndEntityId(entityType, entityId);
        }

        /**
         * Test that ValidationException is thrown when entityType is null.
         * Verifies: input validation for null entityType.
         */
        @Test
        void testGetLogsByEntityNullEntityTypeTest() {
            String entityId = "txn-123";

            assertThatThrownBy(() -> auditLogService.getLogsByEntity(null, entityId))
                    .isInstanceOf(ValidationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "INVALID_INPUT")
                    .hasMessageContaining("entityType");

            verify(auditLogRepository, never()).findByEntityTypeAndEntityId(anyString(), anyString());
        }

        /**
         * Test that ValidationException is thrown when entityType is empty string.
         * Verifies: input validation for empty entityType.
         */
        @Test
        void testGetLogsByEntityEmptyEntityTypeTest() {
            String entityId = "txn-123";

            assertThatThrownBy(() -> auditLogService.getLogsByEntity("", entityId))
                    .isInstanceOf(ValidationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "INVALID_INPUT")
                    .hasMessageContaining("entityType");

            verify(auditLogRepository, never()).findByEntityTypeAndEntityId(anyString(), anyString());
        }

        /**
         * Test that ValidationException is thrown when entityType is whitespace only.
         * Verifies: input validation for whitespace entityType.
         */
        @Test
        void testGetLogsByEntityWhitespaceEntityTypeTest() {
            String entityId = "txn-123";

            assertThatThrownBy(() -> auditLogService.getLogsByEntity("   ", entityId))
                    .isInstanceOf(ValidationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "INVALID_INPUT")
                    .hasMessageContaining("entityType");

            verify(auditLogRepository, never()).findByEntityTypeAndEntityId(anyString(), anyString());
        }

        /**
         * Test that ValidationException is thrown when entityId is null.
         * Verifies: input validation for null entityId.
         */
        @Test
        void testGetLogsByEntityNullEntityIdTest() {
            String entityType = "FinancialTransaction";

            assertThatThrownBy(() -> auditLogService.getLogsByEntity(entityType, null))
                    .isInstanceOf(ValidationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "INVALID_INPUT")
                    .hasMessageContaining("entityId");

            verify(auditLogRepository, never()).findByEntityTypeAndEntityId(anyString(), anyString());
        }

        /**
         * Test that ValidationException is thrown when entityId is empty string.
         * Verifies: input validation for empty entityId.
         */
        @Test
        void testGetLogsByEntityEmptyEntityIdTest() {
            String entityType = "FinancialTransaction";

            assertThatThrownBy(() -> auditLogService.getLogsByEntity(entityType, ""))
                    .isInstanceOf(ValidationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "INVALID_INPUT")
                    .hasMessageContaining("entityId");

            verify(auditLogRepository, never()).findByEntityTypeAndEntityId(anyString(), anyString());
        }

        /**
         * Test that ValidationException is thrown when entityId is whitespace only.
         * Verifies: input validation for whitespace entityId.
         */
        @Test
        void testGetLogsByEntityWhitespaceEntityIdTest() {
            String entityType = "FinancialTransaction";

            assertThatThrownBy(() -> auditLogService.getLogsByEntity(entityType, "   "))
                    .isInstanceOf(ValidationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "INVALID_INPUT")
                    .hasMessageContaining("entityId");

            verify(auditLogRepository, never()).findByEntityTypeAndEntityId(anyString(), anyString());
        }

        /**
         * Test empty list returned when no logs found for entity.
         * Verifies: handles empty result correctly.
         */
        @Test
        void testGetLogsByEntityEmptyResultTest() {
            String entityType = "FinancialTransaction";
            String entityId = "non-existent";

            when(auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId))
                    .thenReturn(Collections.emptyList());

            List<AuditLogResponseDto> result = auditLogService.getLogsByEntity(entityType, entityId);

            assertThat(result)
                    .isNotNull()
                    .isEmpty();

            verify(auditLogRepository, times(1))
                    .findByEntityTypeAndEntityId(entityType, entityId);
        }

        /**
         * Test retrieval with multiple logs in different actions.
         * Verifies: multiple actions are properly mapped.
         */
        @Test
        void testGetLogsByEntityMultipleActionsTest() {
            String entityType = "Payment";
            String entityId = "pay-789";

            List<AuditLog> logs = Arrays.asList(
                    buildAuditLog(1L, entityType, entityId, "CREATE", "alice", "Payment initiated"),
                    buildAuditLog(2L, entityType, entityId, "AUTHORIZE", "bob", "Payment authorized"),
                    buildAuditLog(3L, entityType, entityId, "CAPTURE", "bob", "Payment captured"),
                    buildAuditLog(4L, entityType, entityId, "SETTLE", "system", "Payment settled")
            );

            when(auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId))
                    .thenReturn(logs);

            List<AuditLogResponseDto> result = auditLogService.getLogsByEntity(entityType, entityId);

            assertThat(result)
                    .hasSize(4)
                    .extracting("action")
                    .containsExactly("CREATE", "AUTHORIZE", "CAPTURE", "SETTLE");

            assertThat(result)
                    .extracting("performedBy")
                    .containsExactly("alice", "bob", "bob", "system");
        }

        /**
         * Test that all fields are properly mapped.
         * Verifies: complete DTO mapping from entity.
         */
        @Test
        void testGetLogsByEntityMapsAllFieldsTest() {
            String entityType = "Order";
            String entityId = "ord-456";
            Instant timestamp = Instant.parse("2026-03-17T10:30:00Z");

            AuditLog log = new AuditLog();
            log.setId(100L);
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setAction("PLACED");
            log.setPerformedBy("customer-1");
            log.setPerformedAt(timestamp);
            log.setChangeSummary("Order placed with 5 items");
            log.setCorrelationId("corr-order-123");

            when(auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId))
                    .thenReturn(Collections.singletonList(log));

            List<AuditLogResponseDto> result = auditLogService.getLogsByEntity(entityType, entityId);

            assertThat(result).hasSize(1);
            AuditLogResponseDto dto = result.get(0);
            assertThat(dto)
                    .extracting(
                            "id",
                            "entityType",
                            "entityId",
                            "action",
                            "performedBy",
                            "changeSummary",
                            "correlationId"
                    )
                    .containsExactly(
                            100L,
                            entityType,
                            entityId,
                            "PLACED",
                            "customer-1",
                            "Order placed with 5 items",
                            "corr-order-123"
                    );
            assertThat(dto.getPerformedAt()).isEqualTo(timestamp);
        }
    }

    @Nested
    class GetLogsByPerformedByTests {

        /**
         * Test successful retrieval of logs by performer.
         * Verifies: logs are found and mapped correctly.
         */
        @Test
        void testGetLogsByPerformedBySuccessfullyTest() {
            String performedBy = "admin-user";

            List<AuditLog> logs = Arrays.asList(
                    buildAuditLog(1L, "Transaction", "txn-1", "APPROVE", performedBy, "Approved transaction"),
                    buildAuditLog(2L, "Transaction", "txn-2", "REJECT", performedBy, "Rejected transaction")
            );

            when(auditLogRepository.findByPerformedBy(performedBy))
                    .thenReturn(logs);

            List<AuditLogResponseDto> result = auditLogService.getLogsByPerformedBy(performedBy);

            assertThat(result)
                    .isNotNull()
                    .hasSize(2)
                    .extracting("performedBy")
                    .containsOnly(performedBy);

            verify(auditLogRepository, times(1)).findByPerformedBy(performedBy);
        }

        /**
         * Test that ValidationException is thrown when performedBy is null.
         * Verifies: input validation for null performedBy.
         */
        @Test
        void testGetLogsByPerformedByNullPerformedByTest() {
            assertThatThrownBy(() -> auditLogService.getLogsByPerformedBy(null))
                    .isInstanceOf(ValidationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "INVALID_INPUT")
                    .hasMessageContaining("performedBy");

            verify(auditLogRepository, never()).findByPerformedBy(anyString());
        }

        /**
         * Test that ValidationException is thrown when performedBy is empty string.
         * Verifies: input validation for empty performedBy.
         */
        @Test
        void testGetLogsByPerformedByEmptyPerformedByTest() {
            assertThatThrownBy(() -> auditLogService.getLogsByPerformedBy(""))
                    .isInstanceOf(ValidationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "INVALID_INPUT")
                    .hasMessageContaining("performedBy");

            verify(auditLogRepository, never()).findByPerformedBy(anyString());
        }

        /**
         * Test that ValidationException is thrown when performedBy is whitespace only.
         * Verifies: input validation for whitespace performedBy.
         */
        @Test
        void testGetLogsByPerformedByWhitespacePerformedByTest() {
            assertThatThrownBy(() -> auditLogService.getLogsByPerformedBy("   "))
                    .isInstanceOf(ValidationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "INVALID_INPUT")
                    .hasMessageContaining("performedBy");

            verify(auditLogRepository, never()).findByPerformedBy(anyString());
        }

        /**
         * Test empty list returned when no logs found for performer.
         * Verifies: handles empty result correctly.
         */
        @Test
        void testGetLogsByPerformedByEmptyResultTest() {
            String performedBy = "inactive-user";

            when(auditLogRepository.findByPerformedBy(performedBy))
                    .thenReturn(Collections.emptyList());

            List<AuditLogResponseDto> result = auditLogService.getLogsByPerformedBy(performedBy);

            assertThat(result)
                    .isNotNull()
                    .isEmpty();

            verify(auditLogRepository, times(1)).findByPerformedBy(performedBy);
        }

        /**
         * Test retrieval with multiple logs from same performer.
         * Verifies: multiple logs for same performer are returned.
         */
        @Test
        void testGetLogsByPerformedByMultipleLogsTest() {
            String performedBy = "operator-5";

            List<AuditLog> logs = Arrays.asList(
                    buildAuditLog(1L, "Account", "acc-1", "LOGIN", performedBy, "User logged in"),
                    buildAuditLog(2L, "Transfer", "trn-1", "CREATE", performedBy, "Transfer initiated"),
                    buildAuditLog(3L, "Transfer", "trn-1", "APPROVE", performedBy, "Transfer approved"),
                    buildAuditLog(4L, "Account", "acc-1", "LOGOUT", performedBy, "User logged out")
            );

            when(auditLogRepository.findByPerformedBy(performedBy))
                    .thenReturn(logs);

            List<AuditLogResponseDto> result = auditLogService.getLogsByPerformedBy(performedBy);

            assertThat(result)
                    .hasSize(4)
                    .extracting("action")
                    .containsExactly("LOGIN", "CREATE", "APPROVE", "LOGOUT");

            assertThat(result)
                    .extracting("entityType")
                    .containsExactly("Account", "Transfer", "Transfer", "Account");
        }

        /**
         * Test that all fields are properly mapped for performedBy results.
         * Verifies: complete DTO mapping.
         */
        @Test
        void testGetLogsByPerformedByMapsAllFieldsTest() {
            String performedBy = "manager-xyz";
            Instant timestamp = Instant.parse("2026-03-16T15:45:30Z");

            AuditLog log = new AuditLog();
            log.setId(200L);
            log.setEntityType("Report");
            log.setEntityId("rep-999");
            log.setAction("GENERATE");
            log.setPerformedBy(performedBy);
            log.setPerformedAt(timestamp);
            log.setChangeSummary("Generated monthly report");
            log.setCorrelationId("corr-report-456");

            when(auditLogRepository.findByPerformedBy(performedBy))
                    .thenReturn(Collections.singletonList(log));

            List<AuditLogResponseDto> result = auditLogService.getLogsByPerformedBy(performedBy);

            assertThat(result).hasSize(1);
            AuditLogResponseDto dto = result.get(0);
            assertThat(dto)
                    .extracting(
                            "id",
                            "entityType",
                            "entityId",
                            "action",
                            "performedBy",
                            "changeSummary",
                            "correlationId"
                    )
                    .containsExactly(
                            200L,
                            "Report",
                            "rep-999",
                            "GENERATE",
                            performedBy,
                            "Generated monthly report",
                            "corr-report-456"
                    );
            assertThat(dto.getPerformedAt()).isEqualTo(timestamp);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ░░░░░░░░░░░░░░░░░░░░░░░░░░░ LOG ACTION TESTS ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░
    // ═══════════════════════════════════════════════════════════════════════════════

    @Nested
    class LogActionTests {

        /**
         * Test successful logging of action with correlation ID from MDC.
         * Verifies: audit log is created and saved correctly.
         */
        @Test
        void testLogActionSuccessfullyWithCorrelationIdTest() {
            String entityType = "FinancialTransaction";
            String entityId = "txn-log-1";
            String action = "CREATE";
            String changeSummary = "New transaction created";
            String correlationId = "corr-id-log-001";

            CorrelationIdHolder.setCurrentCorrelationId(correlationId);

            auditLogService.logAction(entityType, entityId, action, changeSummary);

            verify(auditLogRepository, times(1)).save(argThat(log ->
                    entityType.equals(log.getEntityType()) &&
                            entityId.equals(log.getEntityId()) &&
                            action.equals(log.getAction()) &&
                            changeSummary.equals(log.getChangeSummary()) &&
                            correlationId.equals(log.getCorrelationId()) &&
                            log.getPerformedAt() != null &&
                            log.getPerformedBy() != null
            ));

            CorrelationIdHolder.setCurrentCorrelationId(null);
        }

        /**
         * Test logging action without correlation ID (MDC returns null).
         * Verifies: audit log is still created with null correlation ID.
         */
        @Test
        void testLogActionWithoutCorrelationIdTest() {
            String entityType = "Payment";
            String entityId = "pay-log-2";
            String action = "UPDATE_STATUS";
            String changeSummary = "Status updated to CAPTURED";

            CorrelationIdHolder.setCurrentCorrelationId(null);

            auditLogService.logAction(entityType, entityId, action, changeSummary);

            verify(auditLogRepository, times(1)).save(argThat(log ->
                    entityType.equals(log.getEntityType()) &&
                            entityId.equals(log.getEntityId()) &&
                            action.equals(log.getAction()) &&
                            changeSummary.equals(log.getChangeSummary()) &&
                            log.getCorrelationId() == null &&
                            log.getPerformedAt() != null
            ));
        }

        /**
         * Test that performedBy is set to "system" when DefaultAuditorAware returns empty.
         * Verifies: fallback auditor resolution.
         */
        @Test
        void testLogActionPerformedBySystemFallbackTest() {
            String entityType = "Configuration";
            String entityId = "cfg-log-3";
            String action = "UPDATE";
            String changeSummary = "Config parameter updated";

            auditLogService.logAction(entityType, entityId, action, changeSummary);

            verify(auditLogRepository, times(1)).save(argThat(log ->
                    "system".equals(log.getPerformedBy())
            ));
        }

        /**
         * Test that performedAt is always set to current time.
         * Verifies: timestamp is automatically populated.
         */
        @Test
        void testLogActionSetsTimestampTest() {
            String entityType = "Audit";
            String entityId = "aud-log-4";
            String action = "VERIFY";
            String changeSummary = "Audit record verified";

            Instant beforeLogAction = Instant.now();

            auditLogService.logAction(entityType, entityId, action, changeSummary);

            Instant afterLogAction = Instant.now();

            verify(auditLogRepository, times(1)).save(argThat(log -> {
                Instant performedAt = log.getPerformedAt();
                return performedAt != null &&
                        !performedAt.isBefore(beforeLogAction) &&
                        !performedAt.isAfter(afterLogAction);
            }));
        }

        /**
         * Test logging various entity types.
         * Verifies: service works with different entity types.
         */
        @ParameterizedTest
        @ValueSource(strings = {
                "FinancialTransaction",
                "Payment",
                "Order",
                "Invoice",
                "Shipment",
                "Refund"
        })
        void testLogActionVariousEntityTypes(String entityType) {
            String entityId = "entity-id-" + entityType;
            String action = "CREATE";
            String changeSummary = entityType + " created successfully";

            auditLogService.logAction(entityType, entityId, action, changeSummary);

            verify(auditLogRepository, times(1)).save(argThat(log ->
                    entityType.equals(log.getEntityType()) &&
                            entityId.equals(log.getEntityId())
            ));
        }

        /**
         * Test logging various action types.
         * Verifies: service works with different action types.
         */
        @ParameterizedTest
        @ValueSource(strings = {
                "CREATE",
                "UPDATE",
                "DELETE",
                "APPROVE",
                "REJECT",
                "CANCEL",
                "RETRY",
                "EXPORT",
                "IMPORT"
        })
        void testLogActionVariousActionsTest(String action) {
            String entityType = "Record";
            String entityId = "rec-log-action-" + action;
            String changeSummary = "Action " + action + " performed";

            auditLogService.logAction(entityType, entityId, action, changeSummary);

            verify(auditLogRepository, times(1)).save(argThat(log ->
                    action.equals(log.getAction())
            ));
        }

        /**
         * Test logging with long change summary.
         * Verifies: handles large text content.
         */
        @Test
        void testLogActionWithLongChangeSummaryTest() {
            String entityType = "Document";
            String entityId = "doc-long-summary";
            String action = "UPDATE";
            String longSummary = "Updated document with " +
                    "significant changes including new sections, " +
                    "revised content, and updated metadata. " +
                    "All changes have been reviewed and approved.";

            auditLogService.logAction(entityType, entityId, action, longSummary);

            verify(auditLogRepository, times(1)).save(argThat(log ->
                    longSummary.equals(log.getChangeSummary())
            ));
        }

        /**
         * Test logging with special characters in fields.
         * Verifies: handles special characters correctly.
         */
        @Test
        void testLogActionWithSpecialCharactersTest() {
            String entityType = "Transaction_Special@Type";
            String entityId = "id#123$456&789";
            String action = "PROCESS_PAYMENT";
            String changeSummary = "Payment of £999.99 € processed; status: OK ✓";

            auditLogService.logAction(entityType, entityId, action, changeSummary);

            verify(auditLogRepository, times(1)).save(argThat(log ->
                    entityType.equals(log.getEntityType()) &&
                            entityId.equals(log.getEntityId()) &&
                            changeSummary.equals(log.getChangeSummary())
            ));
        }

        /**
         * Test that multiple log actions are independently saved.
         * Verifies: each call results in separate save.
         */
        @Test
        void testMultipleLogActionsIndependentTest() {
            String entityType = "MultiLog";

            auditLogService.logAction(entityType, "id-1", "ACTION_1", "Summary 1");
            auditLogService.logAction(entityType, "id-2", "ACTION_2", "Summary 2");
            auditLogService.logAction(entityType, "id-3", "ACTION_3", "Summary 3");

            verify(auditLogRepository, times(3)).save(any(AuditLog.class));
        }
    }

    @Nested
    class MapToDtoTests {

        /**
         * Test that mapToDto correctly maps all non-null fields.
         * Verifies: complete field mapping.
         */
        @Test
        void testMapToDtoAllFieldsTest() {
            Instant timestamp = Instant.parse("2026-03-17T12:00:00Z");

            AuditLog entity = new AuditLog();
            entity.setId(999L);
            entity.setEntityType("CompleteEntity");
            entity.setEntityId("complete-id-123");
            entity.setAction("FULL_ACTION");
            entity.setPerformedBy("mapper-user");
            entity.setPerformedAt(timestamp);
            entity.setChangeSummary("All fields populated");
            entity.setCorrelationId("corr-mapper-123");

            when(auditLogRepository.findByEntityTypeAndEntityId("CompleteEntity", "complete-id-123"))
                    .thenReturn(Collections.singletonList(entity));

            List<AuditLogResponseDto> result = auditLogService.getLogsByEntity("CompleteEntity", "complete-id-123");

            assertThat(result).hasSize(1);
            AuditLogResponseDto dto = result.get(0);

            assertThat(dto)
                    .extracting(
                            "id",
                            "entityType",
                            "entityId",
                            "action",
                            "performedBy",
                            "changeSummary",
                            "correlationId"
                    )
                    .containsExactly(
                            999L,
                            "CompleteEntity",
                            "complete-id-123",
                            "FULL_ACTION",
                            "mapper-user",
                            "All fields populated",
                            "corr-mapper-123"
                    );

            assertThat(dto.getPerformedAt()).isEqualTo(timestamp);
        }

        /**
         * Test that mapToDto handles null optional fields.
         * Verifies: null fields are preserved in DTO.
         */
        @Test
        void testMapToDtoWithNullOptionalFieldsTest() {
            AuditLog entity = new AuditLog();
            entity.setId(111L);
            entity.setEntityType("MinimalEntity");
            entity.setEntityId("minimal-id");
            entity.setAction("MINIMAL");
            entity.setPerformedBy(null);
            entity.setPerformedAt(Instant.now());
            entity.setChangeSummary(null);
            entity.setCorrelationId(null);

            when(auditLogRepository.findByEntityTypeAndEntityId("MinimalEntity", "minimal-id"))
                    .thenReturn(Collections.singletonList(entity));

            List<AuditLogResponseDto> result = auditLogService.getLogsByEntity("MinimalEntity", "minimal-id");

            assertThat(result).hasSize(1);
            AuditLogResponseDto dto = result.get(0);

            assertThat(dto.getPerformedBy()).isNull();
            assertThat(dto.getChangeSummary()).isNull();
            assertThat(dto.getCorrelationId()).isNull();
            assertThat(dto.getPerformedAt()).isNotNull();
        }

        /**
         * Test builder pattern is working correctly in DTO construction.
         * Verifies: builder creates valid DTO instances.
         */
        @Test
        void testMapToDtoBuilderPatternTest() {
            AuditLog entity = buildAuditLog(
                    555L,
                    "BuilderTest",
                    "builder-id",
                    "BUILD",
                    "builder-user",
                    "Testing builder"
            );

            when(auditLogRepository.findByEntityTypeAndEntityId("BuilderTest", "builder-id"))
                    .thenReturn(Collections.singletonList(entity));

            List<AuditLogResponseDto> result = auditLogService.getLogsByEntity("BuilderTest", "builder-id");

            assertThat(result).hasSize(1);
            AuditLogResponseDto dto = result.get(0);

            assertThat(dto.getId()).isEqualTo(555L);
            assertThat(dto.getEntityType()).isEqualTo("BuilderTest");
            assertThat(dto.getEntityId()).isEqualTo("builder-id");
            assertThat(dto.getAction()).isEqualTo("BUILD");
            assertThat(dto.getPerformedBy()).isEqualTo("builder-user");
            assertThat(dto.getChangeSummary()).isEqualTo("Testing builder");
        }
    }


    /**
     * Helper method to build an AuditLog entity.
     */
    private AuditLog buildAuditLog(
            Long id,
            String entityType,
            String entityId,
            String action,
            String performedBy,
            String changeSummary) {
        AuditLog log = new AuditLog();
        log.setId(id);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setPerformedBy(performedBy);
        log.setPerformedAt(Instant.now());
        log.setChangeSummary(changeSummary);
        log.setCorrelationId("corr-" + id);
        return log;
    }
}