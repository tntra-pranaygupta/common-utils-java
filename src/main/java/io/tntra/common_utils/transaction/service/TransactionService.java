package io.tntra.common_utils.transaction.service;

import io.tntra.common_utils.transaction.dto.TransactionRequestDto;
import io.tntra.common_utils.transaction.dto.TransactionResponseDto;

import java.util.List;

public interface TransactionService {
    TransactionResponseDto createTransaction(TransactionRequestDto requestDto);
    TransactionResponseDto getTransactionByExternalId(String externalId);
    List<TransactionResponseDto> getAllTransactions();
    TransactionResponseDto updateTransactionStatus(String externalId, String status);
}
