package io.tntra.common_utils.transaction.service;

import io.tntra.common_utils.audit.service.AuditLogService;
import io.tntra.common_utils.exception.types.BusinessException;
import io.tntra.common_utils.transaction.dto.TransactionRequestDto;
import io.tntra.common_utils.transaction.dto.TransactionResponseDto;
import io.tntra.common_utils.transaction.entity.FinancialTransaction;
import io.tntra.common_utils.transaction.repository.FinancialTransactionRepository;
import io.tntra.common_utils.util.CorrelationIdHolder;
import io.tntra.common_utils.validation.util.LuhnAlgorithm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {

    private LuhnAlgorithm luhnAlgorithm;
    private final FinancialTransactionRepository repository;
    private final AuditLogService auditLogService;

    public TransactionServiceImpl(FinancialTransactionRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public TransactionResponseDto createTransaction(TransactionRequestDto requestDto) {
        // Explicit validation example: reject transactions exactly equal to 999.99 for fraud simulation
        if (requestDto.getAmount().compareTo(new BigDecimal("999.99")) == 0) {
            throw new io.tntra.common_utils.exception.types.ValidationException("SUSPICIOUS_AMOUNT", "Transactions containing exact 999.99 amount are blocked.");
        }
    
        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setExternalId(UUID.randomUUID().toString());
        transaction.setPanToken(LuhnAlgorithm.maskPan(requestDto.getPanLastFour()));
        transaction.setPanLastFour(requestDto.getPanLastFour());
        transaction.setCardScheme(requestDto.getCardScheme());
        transaction.setAmount(requestDto.getAmount());
        transaction.setCurrency(requestDto.getCurrency());
        transaction.setStatus("PENDING");
        
        String corrId = requestDto.getCorrelationId() != null ? 
            requestDto.getCorrelationId() : CorrelationIdHolder.getCurrentCorrelationId();
        transaction.setCorrelationId(corrId);

        FinancialTransaction saved = repository.save(transaction);
        
        auditLogService.logAction("FinancialTransaction", saved.getExternalId(), "CREATE", 
            "Created transaction with amount " + saved.getAmount() + " " + saved.getCurrency());
            
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponseDto getTransactionByExternalId(String externalId) {
        FinancialTransaction transaction = repository.findByExternalId(externalId)
            .orElseThrow(() -> new BusinessException("NOT_FOUND", "Transaction not found"));
        return mapToDto(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getAllTransactions() {
        return repository.findAll().stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TransactionResponseDto updateTransactionStatus(String externalId, String status) {
        FinancialTransaction transaction = repository.findByExternalId(externalId)
            .orElseThrow(() -> new BusinessException("NOT_FOUND", "Transaction not found"));
            
        String oldStatus = transaction.getStatus();
        
        // Explicit business rule exception: Cannot move a completed transaction back to PENDING.
        if (status.equals("PENDING") && (oldStatus.equals("SETTLED") || oldStatus.equals("CANCELLED") || oldStatus.equals("FAILED"))) {
             throw new BusinessException("INVALID_TRANSITION", "Cannot transition status from " + oldStatus + " to " + status);
        }
        transaction.setStatus(status);
        FinancialTransaction saved = repository.save(transaction);
        
        auditLogService.logAction("FinancialTransaction", saved.getExternalId(), "UPDATE_STATUS", 
            "Status changed from " + oldStatus + " to " + status);
            
        return mapToDto(saved);
    }

    private TransactionResponseDto mapToDto(FinancialTransaction entity) {
        return TransactionResponseDto.builder()
            .externalId(entity.getExternalId())
            .panLastFour(entity.getPanLastFour())
            .cardScheme(entity.getCardScheme())
            .amount(entity.getAmount())
            .currency(entity.getCurrency())
            .status(entity.getStatus())
            .failureReason(entity.getFailureReason())
            .correlationId(entity.getCorrelationId())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}
