package com.example.txnmonitor.rule;

import com.example.txnmonitor.transaction.Transaction;
import com.example.txnmonitor.transaction.TransactionEvaluationRequested;
import com.example.txnmonitor.transaction.TransactionEvaluator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionEvaluationConsumerTest {

	@Test
	void onEvaluationRequested_delegatesToEvaluationService() {
		RecordingTransactionEvaluator evaluator = new RecordingTransactionEvaluator();
		TransactionEvaluationConsumer consumer = new TransactionEvaluationConsumer(evaluator);

		consumer.onEvaluationRequested(new TransactionEvaluationRequested(7L, Instant.now()));

		assertEquals(List.of(7L), evaluator.evaluatedTransactionIds);
	}

	private static final class RecordingTransactionEvaluator implements TransactionEvaluator {

		private final List<Long> evaluatedTransactionIds = new ArrayList<>();

		@Override
		public void evaluate(Transaction transaction) {
		}

		@Override
		public void evaluateByTransactionId(Long transactionId) {
			evaluatedTransactionIds.add(transactionId);
		}
	}
}
