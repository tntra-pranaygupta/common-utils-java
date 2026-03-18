package io.tntra.common_utils.service;

import io.tntra.common_utils.dto.TransactionRequestDto;
import io.tntra.common_utils.dto.TransactionResponseDto;

import java.util.List;

public interface TransactionService {
    TransactionResponseDto createTransaction(TransactionRequestDto requestDto);
    TransactionResponseDto getTransactionByExternalId(String externalId);
    List<TransactionResponseDto> getAllTransactions();
    TransactionResponseDto updateTransactionStatus(String externalId, String status);
}
