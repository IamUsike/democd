package com.example.txnmonitor.transaction;

import com.example.txnmonitor.api.TransactionRequest;
import com.example.txnmonitor.api.TransactionResponse;
import com.example.txnmonitor.common.exception.TransactionNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public TransactionResponse saveTransaction(TransactionRequest request) {
        Transaction savedTransaction = transactionRepository.save(toEntity(request));
        return toResponse(savedTransaction);
    }

    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public TransactionResponse getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        return toResponse(transaction);
    }

    public List<TransactionResponse> searchByAccountId(String accountId) {
        return transactionRepository.findByAccountId(accountId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TransactionResponse> searchByStatus(String status) {
        return transactionRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TransactionResponse> searchByType(String type) {
        return transactionRepository.findByType(type).stream()
                .map(this::toResponse)
                .toList();
    }

    private Transaction toEntity(TransactionRequest request) {
        Transaction transaction = new Transaction();
        transaction.setAccountId(request.getAccountId());
        transaction.setPayeeId(request.getPayeeId());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setType(request.getType());
        transaction.setTimestamp(request.getTimestamp());
        transaction.setDescription(request.getDescription());
        transaction.setStatus(request.getStatus());
        return transaction;
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getTransactionId(),
                transaction.getAccountId(),
                transaction.getPayeeId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getType(),
                transaction.getTimestamp(),
                transaction.getDescription(),
                transaction.getStatus()
        );
    }
}

