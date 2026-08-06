package com.example.txnmonitor.transaction;

@FunctionalInterface
public interface EvaluationMessageSender {

	void send(String exchange, String routingKey, TransactionEvaluationRequested message);
}
