package com.example.txnmonitor.transaction;

public interface TransactionEvaluator {

	void evaluate(Transaction transaction);

	void evaluateByTransactionId(Long transactionId);
}
