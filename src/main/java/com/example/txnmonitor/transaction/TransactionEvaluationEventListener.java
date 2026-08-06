package com.example.txnmonitor.transaction;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(prefix = "txnmonitor.evaluation", name = "mode", havingValue = "async")
public class TransactionEvaluationEventListener {

	private final TransactionEvaluationPublisher evaluationPublisher;

	public TransactionEvaluationEventListener(TransactionEvaluationPublisher evaluationPublisher) {
		this.evaluationPublisher = evaluationPublisher;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onTransactionSaved(TransactionEvaluationRequestedEvent event) {
		evaluationPublisher.publish(event.transactionId());
	}
}
