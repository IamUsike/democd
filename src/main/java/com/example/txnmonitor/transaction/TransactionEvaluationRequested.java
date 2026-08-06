package com.example.txnmonitor.transaction;

import java.time.Instant;

/**
 * Message published to the evaluation queue after a transaction is persisted.
 */
public record TransactionEvaluationRequested(Long transactionId, Instant publishedAt) {
}
