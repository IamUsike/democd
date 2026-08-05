package com.example.txnmonitor.transaction;

import com.example.txnmonitor.alert.AlertService;
import com.example.txnmonitor.api.TransactionRequest;
import com.example.txnmonitor.api.TransactionResponse;
import com.example.txnmonitor.common.exception.TransactionNotFoundException;
import com.example.txnmonitor.rule.RuleEngine;
import com.example.txnmonitor.rule.RuleEvaluationContext;
import com.example.txnmonitor.rule.RuleMatch;
import com.example.txnmonitor.rule.TransactionSnapshot;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final RuleEngine ruleEngine;
    private final RuleEvaluationContext ruleEvaluationContext;
    private final AlertService alertService;

    public TransactionService(
            TransactionRepository transactionRepository,
            RuleEngine ruleEngine,
            RuleEvaluationContext ruleEvaluationContext,
            AlertService alertService) {
        this.transactionRepository = transactionRepository;
        this.ruleEngine = ruleEngine;
        this.ruleEvaluationContext = ruleEvaluationContext;
        this.alertService = alertService;
    }

    public TransactionResponse saveTransaction(TransactionRequest request) {
        Transaction savedTransaction = transactionRepository.save(toEntity(request));
        List<RuleMatch> matches = ruleEngine.evaluate(
                toSnapshot(savedTransaction),
                ruleEvaluationContext);
        alertService.createFromMatches(savedTransaction, matches);
        return toResponse(savedTransaction);
    }

    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TransactionResponse> getTransactions(String sourceType, String sourceId, String accountId) {
        if (hasText(sourceType) && hasText(sourceId) && hasText(accountId)) {
            return transactionRepository.findBySourceTypeAndSourceIdAndAccountId(sourceType, sourceId, accountId)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }
        if (hasText(sourceType) && hasText(sourceId)) {
            return transactionRepository.findBySourceTypeAndSourceId(sourceType, sourceId).stream()
                    .map(this::toResponse)
                    .toList();
        }
        if (hasText(accountId)) {
            return searchByAccountId(accountId);
        }
        return getAllTransactions();
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
        transaction.setSourceType(request.getSourceType());
        transaction.setSourceId(request.getSourceId());
        transaction.setSourceName(request.getSourceName());
        transaction.setAccountId(request.getAccountId());
        transaction.setPayeeId(request.getPayeeId());
        transaction.setPayeeName(request.getPayeeName());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setType(request.getType());
        transaction.setTimestamp(request.getTimestamp());
        transaction.setLocation(request.getLocation());
        transaction.setLatitude(request.getLatitude());
        transaction.setLongitude(request.getLongitude());
        transaction.setDescription(request.getDescription());
        transaction.setStatus(request.getStatus());
        return transaction;
    }

    private TransactionSnapshot toSnapshot(Transaction transaction) {
        return new TransactionSnapshot(
                transaction.getTransactionId(),
                transaction.getAmount(),
                transaction.getAccountId(),
                transaction.getPayeeId(),
                transaction.getTimestamp(),
                transaction.getType());
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getTransactionId(),
                transaction.getSourceType(),
                transaction.getSourceId(),
                transaction.getSourceName(),
                transaction.getAccountId(),
                transaction.getPayeeId(),
                transaction.getPayeeName(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getType(),
                transaction.getTimestamp(),
                transaction.getLocation(),
                transaction.getLatitude(),
                transaction.getLongitude(),
                transaction.getDescription(),
                transaction.getStatus()
        );
    }

    private boolean hasText(String value) {
        return Objects.nonNull(value) && !value.isBlank();
    }
}

