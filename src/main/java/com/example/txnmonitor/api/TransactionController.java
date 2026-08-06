package com.example.txnmonitor.api;

import com.example.txnmonitor.common.ApiResponse;
import com.example.txnmonitor.transaction.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Transaction Management APIs")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @Operation(
            summary = "Create a transaction",
            description = "Creates a new transaction record and returns the saved transaction details."
    )
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse created = transactionService.saveTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Transaction recorded successfully.", created));
    }

    @GetMapping
    @Operation(
            summary = "Get transactions",
            description = "Returns a paginated transaction list. Use afterId to poll only newer rows "
                    + "(delta feed). Default sort is timestamp,desc; afterId defaults to transactionId,asc."
    )
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getTransactions(
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String sourceId,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Long afterId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        PageResponse<TransactionResponse> transactions = transactionService.getTransactions(
                sourceType, sourceId, accountId, q, from, to, afterId, page, size, sort);
        return ResponseEntity.ok(ApiResponse.ok("Transactions retrieved successfully.", transactions));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get transaction by ID",
            description = "Returns a single transaction for the provided transaction ID."
    )
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Transaction retrieved successfully.", transactionService.getTransactionById(id)));
    }

    @GetMapping("/account/{accountId}")
    @Operation(
            summary = "Get transactions by account ID",
            description = "Returns all transactions that belong to the provided account ID."
    )
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionsByAccountId(
            @PathVariable String accountId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Transactions retrieved successfully.",
                transactionService.searchByAccountId(accountId)));
    }

    @GetMapping("/status/{status}")
    @Operation(
            summary = "Get transactions by status",
            description = "Returns all transactions with the provided status value."
    )
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionsByStatus(
            @PathVariable String status) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Transactions retrieved successfully.",
                transactionService.searchByStatus(status)));
    }

    @GetMapping("/type/{type}")
    @Operation(
            summary = "Get transactions by type",
            description = "Returns all transactions of the provided transaction type."
    )
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionsByType(
            @PathVariable String type) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Transactions retrieved successfully.",
                transactionService.searchByType(type)));
    }
}
