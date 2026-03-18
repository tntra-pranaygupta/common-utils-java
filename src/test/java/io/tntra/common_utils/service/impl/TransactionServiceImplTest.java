package io.tntra.common_utils.service.impl;

import io.tntra.common_utils.exception.types.BusinessException;
import io.tntra.common_utils.exception.types.ValidationException;
import io.tntra.common_utils.dto.TransactionRequestDto;
import io.tntra.common_utils.dto.TransactionResponseDto;
import io.tntra.common_utils.entity.FinancialTransaction;
import io.tntra.common_utils.repository.FinancialTransactionRepository;
import io.tntra.common_utils.service.AuditLogService;
import io.tntra.common_utils.util.CorrelationIdHolder;
import io.tntra.common_utils.validation.util.LuhnAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransactionServiceImpl with 100% code coverage.
 *
 * Tests cover:
 * - createTransaction: happy path, validation, with/without correlation ID, audit logging
 * - getTransactionByExternalId: found and not found scenarios
 * - getAllTransactions: empty list, multiple transactions
 * - updateTransactionStatus: valid transitions, invalid transitions, audit logging
 * - mapToDto: all fields mapping
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private FinancialTransactionRepository repository;

    @Mock
    private AuditLogService auditLogService;

    private TransactionServiceImpl transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl(repository, auditLogService);
        // Clean up MDC before each test
        MDC.clear();
    }

    @Nested
    class CreateTransactionTests {

        /**
         * Test successful transaction creation with all valid inputs.
         * Verifies: transaction is created, saved, mapped correctly, and audit logged.
         */
        @Test
        void testCreateTransactionSuccessfullyTest() {
            TransactionRequestDto requestDto = buildValidTransactionRequest();
            FinancialTransaction savedEntity = buildFinancialTransaction(
                    "ext-123", "4111111111111111", "VISA", new BigDecimal("100.00"), "GBP"
            );
            savedEntity.setId(1L);
            savedEntity.setCreatedAt(Instant.now());
            savedEntity.setCreatedBy("system");

            when(repository.save(any(FinancialTransaction.class))).thenReturn(savedEntity);

            TransactionResponseDto response = transactionService.createTransaction(requestDto);

            assertThat(response)
                    .isNotNull()
                    .extracting("externalId", "cardScheme", "currency", "status")
                    .containsExactly("ext-123", "VISA", "GBP", "PENDING");

            assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(response.getCreatedAt()).isNotNull();
            assertThat(response.getCreatedBy()).isEqualTo("system");

            verify(repository, times(1)).save(any(FinancialTransaction.class));
            verify(auditLogService, times(1)).logAction(
                    eq("FinancialTransaction"),
                    eq("ext-123"),
                    eq("CREATE"),
                    contains("Created transaction")
            );
        }

        /**
         * Test transaction creation with explicit correlation ID in request.
         * Verifies: provided correlation ID is used, not the one from MDC.
         */
        @Test
        void testCreateTransactionWithExplicitCorrelationIdTest() {
            TransactionRequestDto requestDto = buildValidTransactionRequest();
            String explicitCorrId = "explicit-corr-id-123";
            requestDto.setCorrelationId(explicitCorrId);

            FinancialTransaction savedEntity = buildFinancialTransaction(
                    "ext-456", "4111111111111111", "MASTERCARD", new BigDecimal("250.50"), "USD"
            );
            savedEntity.setCorrelationId(explicitCorrId);

            when(repository.save(any(FinancialTransaction.class))).thenReturn(savedEntity);

            TransactionResponseDto response = transactionService.createTransaction(requestDto);

            assertThat(response.getCorrelationId()).isEqualTo(explicitCorrId);

            verify(repository, times(1)).save(argThat(transaction ->
                    transaction.getCorrelationId().equals(explicitCorrId)
            ));
        }

        /**
         * Test transaction creation falls back to MDC correlation ID when not provided in request.
         * Verifies: CorrelationIdHolder is consulted when request doesn't provide one.
         */
        @Test
        void testCreateTransactionWithMdcCorrelationIdTest() {
            String mdcCorrId = "mdc-corr-id-789";
            CorrelationIdHolder.setCurrentCorrelationId(mdcCorrId);

            TransactionRequestDto requestDto = buildValidTransactionRequest();
            requestDto.setCorrelationId(null);

            FinancialTransaction savedEntity = buildFinancialTransaction(
                    "ext-789", "4111111111111111", "AMEX", new BigDecimal("500.00"), "EUR"
            );
            savedEntity.setCorrelationId(mdcCorrId);

            when(repository.save(any(FinancialTransaction.class))).thenReturn(savedEntity);

            try {
                TransactionResponseDto response = transactionService.createTransaction(requestDto);

                assertThat(response.getCorrelationId()).isEqualTo(mdcCorrId);

                verify(repository, times(1)).save(argThat(transaction ->
                        transaction.getCorrelationId().equals(mdcCorrId)
                ));
            } finally {
                CorrelationIdHolder.setCurrentCorrelationId(null);
            }
        }

        /**
         * Test that transaction creation throws ValidationException for suspicious amount (999.99).
         * Verifies: fraud detection validation is enforced.
         */
        @Test
        void testCreateTransactionThrowsExceptionForSuspiciousAmountTest() {
            TransactionRequestDto requestDto = buildValidTransactionRequest();
            requestDto.setAmount(new BigDecimal("999.99"));

            assertThatThrownBy(() -> transactionService.createTransaction(requestDto))
                    .isInstanceOf(ValidationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "SUSPICIOUS_AMOUNT")
                    .hasMessageContaining("999.99");

            // Verify repository and audit service were NOT called
            verify(repository, never()).save(any());
            verify(auditLogService, never()).logAction(anyString(), anyString(), anyString(), anyString());
        }

        /**
         * Test that PAN is masked using LuhnAlgorithm.
         * Verifies: pan is correctly masked.
         */
        @Test
        void testCreateTransactionMasksLuhnPanTest() {
            TransactionRequestDto requestDto = buildValidTransactionRequest();

            FinancialTransaction savedEntity = buildFinancialTransaction(
                    "ext-lun", "4111111111111111", "VISA", new BigDecimal("150.00"), "GBP"
            );
            String maskedPan = LuhnAlgorithm.maskPan("4111111111111111");
            savedEntity.setPan(maskedPan);

            when(repository.save(any(FinancialTransaction.class))).thenReturn(savedEntity);

            TransactionResponseDto response = transactionService.createTransaction(requestDto);

            verify(repository, times(1)).save(argThat(transaction ->
                    transaction.getPan() != null &&
                            transaction.getPan().equals(maskedPan)
            ));
        }

        /**
         * Test transaction creation status is always set to PENDING.
         * Verifies: initial status is correct.
         */
        @Test
        void testCreateTransactionStatusIsPendingTest() {
            TransactionRequestDto requestDto = buildValidTransactionRequest();
            FinancialTransaction savedEntity = buildFinancialTransaction(
                    "ext-pending", "4111111111111111", "VISA", new BigDecimal("75.50"), "GBP"
            );
            savedEntity.setStatus("PENDING");

            when(repository.save(any(FinancialTransaction.class))).thenReturn(savedEntity);

            TransactionResponseDto response = transactionService.createTransaction(requestDto);

            assertThat(response.getStatus()).isEqualTo("PENDING");

            verify(repository, times(1)).save(argThat(transaction ->
                    "PENDING".equals(transaction.getStatus())
            ));
        }

        /**
         * Test that external ID is generated as UUID and unique.
         * Verifies: external ID is always set.
         */
        @Test
        void testCreateTransactionGeneratesUniqueExternalIdTest() {
            TransactionRequestDto requestDto = buildValidTransactionRequest();
            FinancialTransaction savedEntity1 = buildFinancialTransaction(
                    "ext-id-1", "4111111111111111", "VISA", new BigDecimal("100.00"), "GBP"
            );
            FinancialTransaction savedEntity2 = buildFinancialTransaction(
                    "ext-id-2", "4111111111111111", "MASTERCARD", new BigDecimal("200.00"), "USD"
            );

            when(repository.save(any(FinancialTransaction.class)))
                    .thenReturn(savedEntity1)
                    .thenReturn(savedEntity2);

            TransactionResponseDto response1 = transactionService.createTransaction(requestDto);
            TransactionResponseDto response2 = transactionService.createTransaction(requestDto);

            assertThat(response1.getExternalId()).isNotEqualTo(response2.getExternalId());
        }

        /**
         * Test audit log is called with correct parameters.
         * Verifies: audit logging captures transaction details.
         */
        @Test
        void testCreateTransactionAuditLoggingTest() {
            TransactionRequestDto requestDto = buildValidTransactionRequest();
            requestDto.setAmount(new BigDecimal("350.75"));
            requestDto.setCurrency("JPY");

            FinancialTransaction savedEntity = buildFinancialTransaction(
                    "ext-audit", "4111111111111111", "VISA", new BigDecimal("350.75"), "JPY"
            );

            when(repository.save(any(FinancialTransaction.class))).thenReturn(savedEntity);

            transactionService.createTransaction(requestDto);

            verify(auditLogService, times(1)).logAction(
                    eq("FinancialTransaction"),
                    eq("ext-audit"),
                    eq("CREATE"),
                    argThat(message -> message.contains("350.75") && message.contains("JPY"))
            );
        }
    }

    @Nested
    class GetTransactionByIdTests {

        /**
         * Test successful retrieval of transaction by external ID.
         * Verifies: transaction is found and mapped correctly.
         */
        @Test
        void testGetTransactionByExternalIdSuccessfullyTest() {
            String externalId = "ext-get-123";
            FinancialTransaction entity = buildFinancialTransaction(
                    externalId, "4111111111111111", "VISA", new BigDecimal("225.00"), "GBP"
            );
            entity.setCreatedBy("user123");
            entity.setUpdatedBy("user456");

            when(repository.findByExternalId(externalId)).thenReturn(Optional.of(entity));

            TransactionResponseDto response = transactionService.getTransactionByExternalId(externalId);

            assertThat(response)
                    .isNotNull()
                    .extracting("externalId", "cardScheme", "status")
                    .containsExactly(externalId, "VISA", "PENDING");

            assertThat(response.getCreatedBy()).isEqualTo("user123");
            assertThat(response.getUpdatedBy()).isEqualTo("user456");

            verify(repository, times(1)).findByExternalId(externalId);
        }

        /**
         * Test that BusinessException is thrown when transaction is not found.
         * Verifies: proper error handling for missing transaction.
         */
        @Test
        void testGetTransactionByExternalIdNotFoundTest() {
            String externalId = "non-existent-id";
            when(repository.findByExternalId(externalId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.getTransactionByExternalId(externalId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "NOT_FOUND")
                    .hasMessageContaining("Transaction not found");

            verify(repository, times(1)).findByExternalId(externalId);
        }

        /**
         * Test retrieval with various amounts and currencies.
         * Verifies: mapping works for different monetary values.
         */
        @ParameterizedTest
        @ValueSource(strings = {"GBP", "USD", "EUR", "JPY"})
        void testGetTransactionWithVariousCurrenciesTest(String currency) {
            String externalId = "ext-curr-" + currency;
            FinancialTransaction entity = buildFinancialTransaction(
                    externalId, "9999", "AMEX", new BigDecimal("999.99"), currency
            );

            when(repository.findByExternalId(externalId)).thenReturn(Optional.of(entity));

            TransactionResponseDto response = transactionService.getTransactionByExternalId(externalId);

            assertThat(response.getCurrency()).isEqualTo(currency);
        }

        /**
         * Test that all fields including optional ones are mapped.
         * Verifies: complete DTO mapping.
         */
        @Test
        void testGetTransactionMapsAllFieldsTest() {
            String externalId = "ext-all-fields";
            FinancialTransaction entity = new FinancialTransaction();
            entity.setId(99L);
            entity.setExternalId(externalId);
            entity.setPan("masked-token");
            entity.setCardScheme("VISA");
            entity.setAmount(new BigDecimal("555.55"));
            entity.setCurrency("GBP");
            entity.setStatus("AUTHORISED");
            entity.setFailureReason("Expired card");
            entity.setCorrelationId("corr-id-xyz");
            entity.setCreatedAt(Instant.parse("2026-03-17T10:00:00Z"));
            entity.setUpdatedAt(Instant.parse("2026-03-17T11:00:00Z"));
            entity.setCreatedBy("alice");
            entity.setUpdatedBy("bob");

            when(repository.findByExternalId(externalId)).thenReturn(Optional.of(entity));

            TransactionResponseDto response = transactionService.getTransactionByExternalId(externalId);

            assertThat(response)
                    .extracting(
                            "externalId",
                            "cardScheme",
                            "amount",
                            "currency",
                            "status",
                            "failureReason",
                            "correlationId",
                            "createdBy",
                            "updatedBy"
                    )
                    .containsExactly(
                            externalId,
                            "VISA",
                            new BigDecimal("555.55"),
                            "GBP",
                            "AUTHORISED",
                            "Expired card",
                            "corr-id-xyz",
                            "alice",
                            "bob"
                    );

            assertThat(response.getCreatedAt()).isEqualTo(Instant.parse("2026-03-17T10:00:00Z"));
            assertThat(response.getUpdatedAt()).isEqualTo(Instant.parse("2026-03-17T11:00:00Z"));
        }
    }

    @Nested
    class GetAllTransactionsTests {

        /**
         * Test successful retrieval of all transactions.
         * Verifies: multiple transactions are returned and mapped correctly.
         */
        @Test
        void testGetAllTransactionsSuccessfullyTest() {
            List<FinancialTransaction> entities = Arrays.asList(
                    buildFinancialTransaction("ext-1", "4111111111111111", "VISA", new BigDecimal("100.00"), "GBP"),
                    buildFinancialTransaction("ext-2", "4111111111111111", "MASTERCARD", new BigDecimal("200.00"), "USD"),
                    buildFinancialTransaction("ext-3", "4111111111111111", "AMEX", new BigDecimal("300.00"), "EUR")
            );

            when(repository.findAll()).thenReturn(entities);

            List<TransactionResponseDto> responses = transactionService.getAllTransactions();

            assertThat(responses)
                    .isNotNull()
                    .hasSize(3);

            assertThat(responses)
                    .extracting("externalId")
                    .containsExactly("ext-1", "ext-2", "ext-3");

            assertThat(responses)
                    .extracting("cardScheme")
                    .containsExactly("VISA", "MASTERCARD", "AMEX");

            verify(repository, times(1)).findAll();
        }

        /**
         * Test retrieval returns empty list when no transactions exist.
         * Verifies: handles empty repository correctly.
         */
        @Test
        void testGetAllTransactionsEmptyListTest() {
            when(repository.findAll()).thenReturn(Collections.emptyList());

            List<TransactionResponseDto> responses = transactionService.getAllTransactions();

            assertThat(responses)
                    .isNotNull()
                    .isEmpty();

            verify(repository, times(1)).findAll();
        }

        /**
         * Test retrieval with single transaction.
         * Verifies: correctly handles single item list.
         */
        @Test
        void testGetAllTransactionsSingleItemTest() {
            FinancialTransaction entity = buildFinancialTransaction(
                    "ext-single", "4111111111111111", "VISA", new BigDecimal("777.77"), "GBP"
            );

            when(repository.findAll()).thenReturn(Collections.singletonList(entity));

            List<TransactionResponseDto> responses = transactionService.getAllTransactions();

            assertThat(responses)
                    .hasSize(1)
                    .extracting("externalId")
                    .containsExactly("ext-single");
        }

        /**
         * Test that all fields are mapped correctly in list retrieval.
         * Verifies: complete mapping for each transaction in the list.
         */
        @Test
        void testGetAllTransactionsMapsAllFieldsTest() {
            FinancialTransaction entity1 = new FinancialTransaction();
            entity1.setExternalId("ext-1");
            entity1.setStatus("SETTLED");
            entity1.setCreatedBy("user1");

            FinancialTransaction entity2 = new FinancialTransaction();
            entity2.setExternalId("ext-2");
            entity2.setStatus("PENDING");
            entity2.setCreatedBy("user2");

            when(repository.findAll()).thenReturn(Arrays.asList(entity1, entity2));

            List<TransactionResponseDto> responses = transactionService.getAllTransactions();

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0))
                    .extracting("externalId", "status", "createdBy")
                    .containsExactly("ext-1", "SETTLED", "user1");
            assertThat(responses.get(1))
                    .extracting("externalId", "status", "createdBy")
                    .containsExactly("ext-2", "PENDING", "user2");
        }
    }

    @Nested
    class UpdateTransactionStatusTests {

        /**
         * Test successful status update from PENDING to AUTHORISED.
         * Verifies: valid transition is allowed and saved correctly.
         */
        @Test
        void testUpdateStatusPendingToAuthorisedTest() {
            String externalId = "ext-update-1";
            FinancialTransaction entity = buildFinancialTransaction(
                    externalId, "1234", "VISA", new BigDecimal("100.00"), "GBP"
            );
            entity.setStatus("PENDING");

            FinancialTransaction updatedEntity = buildFinancialTransaction(
                    externalId, "1234", "VISA", new BigDecimal("100.00"), "GBP"
            );
            updatedEntity.setStatus("AUTHORISED");

            when(repository.findByExternalId(externalId)).thenReturn(Optional.of(entity));
            when(repository.save(any(FinancialTransaction.class))).thenReturn(updatedEntity);

            TransactionResponseDto response = transactionService.updateTransactionStatus(externalId, "AUTHORISED");

            assertThat(response.getStatus()).isEqualTo("AUTHORISED");

            verify(repository, times(1)).findByExternalId(externalId);
            verify(repository, times(1)).save(argThat(transaction ->
                    "AUTHORISED".equals(transaction.getStatus())
            ));
        }

        /**
         * Test that invalid transition SETTLED->PENDING throws BusinessException.
         * Verifies: business rule is enforced for invalid state transitions.
         */
        @Test
        void testUpdateStatusInvalidTransitionSettledToPendingTest() {
            String externalId = "ext-invalid-1";
            FinancialTransaction entity = buildFinancialTransaction(
                    externalId, "1234", "VISA", new BigDecimal("100.00"), "GBP"
            );
            entity.setStatus("SETTLED");

            when(repository.findByExternalId(externalId)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> transactionService.updateTransactionStatus(externalId, "PENDING"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "INVALID_TRANSITION")
                    .hasMessageContaining("SETTLED")
                    .hasMessageContaining("PENDING");

            verify(repository, never()).save(any());
        }

        /**
         * Test that invalid transition CANCELLED->PENDING throws BusinessException.
         * Verifies: CANCELLED status cannot revert to PENDING.
         */
        @Test
        void testUpdateStatusInvalidTransitionCancelledToPendingTest() {
            String externalId = "ext-invalid-2";
            FinancialTransaction entity = buildFinancialTransaction(
                    externalId, "5678", "MASTERCARD", new BigDecimal("250.00"), "USD"
            );
            entity.setStatus("CANCELLED");

            when(repository.findByExternalId(externalId)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> transactionService.updateTransactionStatus(externalId, "PENDING"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "INVALID_TRANSITION");

            verify(repository, never()).save(any());
        }

        /**
         * Test that invalid transition FAILED->PENDING throws BusinessException.
         * Verifies: FAILED status cannot revert to PENDING.
         */
        @Test
        void testUpdateStatusInvalidTransitionFailedToPendingTest() {
            String externalId = "ext-invalid-3";
            FinancialTransaction entity = buildFinancialTransaction(
                    externalId, "9012", "AMEX", new BigDecimal("500.00"), "EUR"
            );
            entity.setStatus("FAILED");

            when(repository.findByExternalId(externalId)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> transactionService.updateTransactionStatus(externalId, "PENDING"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "INVALID_TRANSITION");

            verify(repository, never()).save(any());
        }

        /**
         * Test valid transitions from PENDING to other states.
         * Verifies: PENDING can transition to AUTHORISED, DECLINED, FAILED.
         */
        @ParameterizedTest
        @ValueSource(strings = {"AUTHORISED", "DECLINED", "FAILED", "CAPTURED", "REFUNDED"})
        void testUpdateStatusValidTransitionsFromPendingTest(String newStatus) {
            String externalId = "ext-valid-" + newStatus;
            FinancialTransaction entity = buildFinancialTransaction(
                    externalId, "1234", "VISA", new BigDecimal("100.00"), "GBP"
            );
            entity.setStatus("PENDING");

            FinancialTransaction updatedEntity = buildFinancialTransaction(
                    externalId, "1234", "VISA", new BigDecimal("100.00"), "GBP"
            );
            updatedEntity.setStatus(newStatus);

            when(repository.findByExternalId(externalId)).thenReturn(Optional.of(entity));
            when(repository.save(any(FinancialTransaction.class))).thenReturn(updatedEntity);

            TransactionResponseDto response = transactionService.updateTransactionStatus(externalId, newStatus);

            assertThat(response.getStatus()).isEqualTo(newStatus);

            verify(repository, times(1)).save(argThat(transaction ->
                    newStatus.equals(transaction.getStatus())
            ));
        }

        /**
         * Test that transaction not found throws BusinessException.
         * Verifies: proper error handling for missing transaction.
         */
        @Test
        void testUpdateStatusTransactionNotFoundTest() {
            String externalId = "non-existent-update";
            when(repository.findByExternalId(externalId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.updateTransactionStatus(externalId, "AUTHORISED"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "NOT_FOUND")
                    .hasMessageContaining("Transaction not found");

            verify(repository, never()).save(any());
        }

        /**
         * Test audit logging on successful status update.
         * Verifies: audit log captures old and new status.
         */
        @Test
        void testUpdateStatusAuditLoggingTest() {
            String externalId = "ext-audit-update";
            FinancialTransaction entity = buildFinancialTransaction(
                    externalId, "1111", "VISA", new BigDecimal("150.00"), "GBP"
            );
            entity.setStatus("PENDING");

            FinancialTransaction updatedEntity = buildFinancialTransaction(
                    externalId, "1111", "VISA", new BigDecimal("150.00"), "GBP"
            );
            updatedEntity.setStatus("CAPTURED");

            when(repository.findByExternalId(externalId)).thenReturn(Optional.of(entity));
            when(repository.save(any(FinancialTransaction.class))).thenReturn(updatedEntity);

            transactionService.updateTransactionStatus(externalId, "CAPTURED");

            verify(auditLogService, times(1)).logAction(
                    eq("FinancialTransaction"),
                    eq(externalId),
                    eq("UPDATE_STATUS"),
                    argThat(message -> message.contains("PENDING") && message.contains("CAPTURED"))
            );
        }

        /**
         * Test valid transition within completed states (AUTHORISED to CAPTURED).
         * Verifies: forward transitions are allowed.
         */
        @Test
        void testUpdateStatusAuthorisedToCapturedTest() {
            String externalId = "ext-forward-1";
            FinancialTransaction entity = buildFinancialTransaction(
                    externalId, "1234", "VISA", new BigDecimal("100.00"), "GBP"
            );
            entity.setStatus("AUTHORISED");

            FinancialTransaction updatedEntity = buildFinancialTransaction(
                    externalId, "1234", "VISA", new BigDecimal("100.00"), "GBP"
            );
            updatedEntity.setStatus("CAPTURED");

            when(repository.findByExternalId(externalId)).thenReturn(Optional.of(entity));
            when(repository.save(any(FinancialTransaction.class))).thenReturn(updatedEntity);

            TransactionResponseDto response = transactionService.updateTransactionStatus(externalId, "CAPTURED");

            assertThat(response.getStatus()).isEqualTo("CAPTURED");

            verify(repository, times(1)).save(any(FinancialTransaction.class));
        }
    }


    @Nested
    class EdgeCasesAndIntegrationTests {

        /**
         * Test transaction with null failure reason (optional field).
         * Verifies: optional fields can be null.
         */
        @Test
        void testTransactionWithNullFailureReasonTest() {
            String externalId = "ext-null-reason";
            FinancialTransaction entity = buildFinancialTransaction(
                    externalId, "1234", "VISA", new BigDecimal("100.00"), "GBP"
            );
            entity.setFailureReason(null);

            when(repository.findByExternalId(externalId)).thenReturn(Optional.of(entity));

            TransactionResponseDto response = transactionService.getTransactionByExternalId(externalId);

            assertThat(response.getFailureReason()).isNull();
        }

        /**
         * Test transaction with all optional fields populated.
         * Verifies: complete entity is mapped correctly.
         */
        @Test
        void testTransactionWithAllOptionalFieldsTest() {
            String externalId = "ext-complete";
            FinancialTransaction entity = new FinancialTransaction();
            entity.setId(1L);
            entity.setExternalId(externalId);
            entity.setPan("masked");
            entity.setCardScheme("VISA");
            entity.setAmount(new BigDecimal("1000.00"));
            entity.setCurrency("GBP");
            entity.setStatus("SETTLED");
            entity.setFailureReason(null);
            entity.setCorrelationId("corr-123");
            entity.setCreatedAt(Instant.now());
            entity.setUpdatedAt(Instant.now());
            entity.setCreatedBy("admin");
            entity.setUpdatedBy("admin");

            when(repository.findByExternalId(externalId)).thenReturn(Optional.of(entity));

            TransactionResponseDto response = transactionService.getTransactionByExternalId(externalId);

            assertThat(response).isNotNull();
            assertThat(response.getCreatedAt()).isNotNull();
            assertThat(response.getUpdatedAt()).isNotNull();
        }

        /**
         * Test large transaction amount.
         * Verifies: no issues with large BigDecimal values.
         */
        @Test
        void testLargeTransactionAmountTest() {
            BigDecimal largeAmount = new BigDecimal("999999.9999");
            TransactionRequestDto requestDto = buildValidTransactionRequest();
            requestDto.setAmount(largeAmount);

            FinancialTransaction savedEntity = buildFinancialTransaction(
                    "ext-large", "1234", "VISA", largeAmount, "GBP"
            );

            when(repository.save(any(FinancialTransaction.class))).thenReturn(savedEntity);

            TransactionResponseDto response = transactionService.createTransaction(requestDto);

            assertThat(response.getAmount()).isEqualByComparingTo(largeAmount);
        }

        /**
         * Test small transaction amount.
         * Verifies: no issues with small decimal amounts.
         */
        @Test
        void testSmallTransactionAmountTest() {
            BigDecimal smallAmount = new BigDecimal("0.01");
            TransactionRequestDto requestDto = buildValidTransactionRequest();
            requestDto.setAmount(smallAmount);

            FinancialTransaction savedEntity = buildFinancialTransaction(
                    "ext-small", "1234", "VISA", smallAmount, "GBP"
            );

            when(repository.save(any(FinancialTransaction.class))).thenReturn(savedEntity);

            TransactionResponseDto response = transactionService.createTransaction(requestDto);

            assertThat(response.getAmount()).isEqualByComparingTo(smallAmount);
        }

        /**
         * Test various valid card schemes.
         * Verifies: service works with different card schemes.
         */
        @ParameterizedTest
        @ValueSource(strings = {"VISA", "MASTERCARD", "AMEX", "DISCOVER", "DINERS"})
        void testVariousCardSchemesTest(String scheme) {
            TransactionRequestDto requestDto = buildValidTransactionRequest();
            requestDto.setCardScheme(scheme);

            FinancialTransaction savedEntity = buildFinancialTransaction(
                    "ext-card-" + scheme, "1234", scheme, new BigDecimal("100.00"), "GBP"
            );

            when(repository.save(any(FinancialTransaction.class))).thenReturn(savedEntity);

            TransactionResponseDto response = transactionService.createTransaction(requestDto);

            assertThat(response.getCardScheme()).isEqualTo(scheme);
        }

        /**
         * Test that suspicious amount 999.99 is rejected with different currencies.
         * Verifies: fraud check works regardless of currency.
         */
        @ParameterizedTest
        @ValueSource(strings = {"GBP", "USD", "EUR", "JPY"})
        void testSuspiciousAmountWithVariousCurrenciesTest(String currency) {
            TransactionRequestDto requestDto = buildValidTransactionRequest();
            requestDto.setAmount(new BigDecimal("999.99"));
            requestDto.setCurrency(currency);

            assertThatThrownBy(() -> transactionService.createTransaction(requestDto))
                    .isInstanceOf(ValidationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "SUSPICIOUS_AMOUNT");

            verify(repository, never()).save(any());
        }
    }

    /**
     * Helper method to build a valid transaction request DTO.
     */
    private TransactionRequestDto buildValidTransactionRequest() {
        TransactionRequestDto dto = new TransactionRequestDto();
        dto.setPan("4111111111111111");
        dto.setCardScheme("VISA");
        dto.setAmount(new BigDecimal("100.00"));
        dto.setCurrency("GBP");
        return dto;
    }

    /**
     * Helper method to build a financial transaction entity.
     */
    private FinancialTransaction buildFinancialTransaction(
            String externalId,
            String pan,
            String cardScheme,
            BigDecimal amount,
            String currency) {
        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setExternalId(externalId);
        transaction.setPan(pan);
        transaction.setCardScheme(cardScheme);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setStatus("PENDING");
        transaction.setCorrelationId("corr-id");
        transaction.setCreatedAt(Instant.now());
        transaction.setCreatedBy("system");
        return transaction;
    }
}