package com.example.txnmonitor.transaction;

/**
 * Fired after a transaction is saved; handled after commit when evaluation is async.
 */
public record TransactionEvaluationRequestedEvent(Long transactionId) {
}
