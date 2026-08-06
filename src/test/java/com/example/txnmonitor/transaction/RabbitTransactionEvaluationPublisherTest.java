package com.example.txnmonitor.transaction;

import com.example.txnmonitor.common.config.RabbitMqConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RabbitTransactionEvaluationPublisherTest {

	@Test
	void publish_sendsEvaluationRequestedMessage() {
		RecordingMessageSender messageSender = new RecordingMessageSender();
		RabbitTransactionEvaluationPublisher publisher = new RabbitTransactionEvaluationPublisher(messageSender);

		publisher.publish(42L);

		assertEquals(RabbitMqConfig.TXN_EVENTS_EXCHANGE, messageSender.exchange);
		assertEquals(RabbitMqConfig.EVALUATION_ROUTING_KEY, messageSender.routingKey);
		assertNotNull(messageSender.message);
		assertEquals(42L, messageSender.message.transactionId());
		assertNotNull(messageSender.message.publishedAt());
	}

	private static final class RecordingMessageSender implements EvaluationMessageSender {

		private String exchange;
		private String routingKey;
		private TransactionEvaluationRequested message;

		@Override
		public void send(String exchange, String routingKey, TransactionEvaluationRequested message) {
			this.exchange = exchange;
			this.routingKey = routingKey;
			this.message = message;
		}
	}
}
