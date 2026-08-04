package com.example.txnmonitor.api;

import com.example.txnmonitor.transaction.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.saveTransaction(request));
    }

    @GetMapping
    @Operation(
            summary = "Get all transactions",
            description = "Returns all transactions, with optional source/account filters."
    )
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String sourceId,
            @RequestParam(required = false) String accountId) {
        return ResponseEntity.ok(transactionService.getTransactions(sourceType, sourceId, accountId));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get transaction by ID",
            description = "Returns a single transaction for the provided transaction ID."
    )
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    @GetMapping("/account/{accountId}")
    @Operation(
            summary = "Get transactions by account ID",
            description = "Returns all transactions that belong to the provided account ID."
    )
    public ResponseEntity<List<TransactionResponse>> getTransactionsByAccountId(@PathVariable String accountId) {
        return ResponseEntity.ok(transactionService.searchByAccountId(accountId));
    }

    @GetMapping("/status/{status}")
    @Operation(
            summary = "Get transactions by status",
            description = "Returns all transactions with the provided status value."
    )
    public ResponseEntity<List<TransactionResponse>> getTransactionsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(transactionService.searchByStatus(status));
    }

    @GetMapping("/type/{type}")
    @Operation(
            summary = "Get transactions by type",
            description = "Returns all transactions of the provided transaction type."
    )
    public ResponseEntity<List<TransactionResponse>> getTransactionsByType(@PathVariable String type) {
        return ResponseEntity.ok(transactionService.searchByType(type));
    }
}

