package com.example.txnmonitor.rule;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.example.txnmonitor.common.config.RabbitMqConfig;
import com.example.txnmonitor.transaction.TransactionEvaluationRequested;
import com.example.txnmonitor.transaction.TransactionEvaluator;

@Component
@ConditionalOnProperty(prefix = "txnmonitor.evaluation", name = "mode", havingValue = "async")
public class TransactionEvaluationConsumer {

	private final TransactionEvaluator transactionEvaluator;

	public TransactionEvaluationConsumer(TransactionEvaluator transactionEvaluator) {
		this.transactionEvaluator = transactionEvaluator;
	}

	@RabbitListener(queues = RabbitMqConfig.EVALUATION_QUEUE)
	public void onEvaluationRequested(TransactionEvaluationRequested message) {
		transactionEvaluator.evaluateByTransactionId(message.transactionId());
	}
}
