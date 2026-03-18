package io.tntra.common_utils.controller;

import io.tntra.common_utils.response.factory.ResponseFactory;
import io.tntra.common_utils.response.model.ApiResponse;
import io.tntra.common_utils.dto.TransactionRequestDto;
import io.tntra.common_utils.dto.TransactionResponseDto;
import io.tntra.common_utils.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final ResponseFactory responseFactory;

    public TransactionController(TransactionService transactionService, ResponseFactory responseFactory) {
        this.transactionService = transactionService;
        this.responseFactory = responseFactory;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponseDto>> createTransaction(
            @Valid @RequestBody TransactionRequestDto requestDto) {
        TransactionResponseDto response = transactionService.createTransaction(requestDto);
        return responseFactory.created(response);
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> getTransaction(
            @PathVariable String externalId) {
        TransactionResponseDto response = transactionService.getTransactionByExternalId(externalId);
        return responseFactory.success(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionResponseDto>>> getAllTransactions() {
        List<TransactionResponseDto> response = transactionService.getAllTransactions();
        return responseFactory.success(response);
    }

    @PatchMapping("/{externalId}/status")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> updateTransactionStatus(
            @PathVariable String externalId,
            @RequestParam String status) {
        TransactionResponseDto response = transactionService.updateTransactionStatus(externalId, status);
        return responseFactory.success(response);
    }
}
