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
            description = "Records an inbound transaction event from a bank or merchant source and returns the persisted transaction payload."
    )
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.saveTransaction(request));
    }

    @GetMapping
    @Operation(
            summary = "Get all transactions",
            description = "Returns all recorded transactions for dashboard listing and operator review."
    )
    public ResponseEntity<List<TransactionResponse>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get transaction by ID",
            description = "Returns one transaction by its transaction ID."
    )
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    @GetMapping("/account/{accountId}")
    @Operation(
            summary = "Get transactions by account ID",
            description = "Returns transactions for the specified account identifier."
    )
    public ResponseEntity<List<TransactionResponse>> getTransactionsByAccountId(@PathVariable String accountId) {
        return ResponseEntity.ok(transactionService.searchByAccountId(accountId));
    }

    @GetMapping("/status/{status}")
    @Operation(
            summary = "Get transactions by status",
            description = "Returns transactions filtered by lifecycle status."
    )
    public ResponseEntity<List<TransactionResponse>> getTransactionsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(transactionService.searchByStatus(status));
    }

    @GetMapping("/type/{type}")
    @Operation(
            summary = "Get transactions by type",
            description = "Returns transactions filtered by transaction type."
    )
    public ResponseEntity<List<TransactionResponse>> getTransactionsByType(@PathVariable String type) {
        return ResponseEntity.ok(transactionService.searchByType(type));
    }
}

