package com.example.txnmonitor.transaction;

@FunctionalInterface
public interface TransactionEvaluationPublisher {

	void publish(Long transactionId);
}
