package com.example.txnmonitor.transaction;

import java.time.Instant;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.example.txnmonitor.common.config.RabbitMqConfig;

@Component
@ConditionalOnProperty(prefix = "txnmonitor.evaluation", name = "mode", havingValue = "async")
public class RabbitTransactionEvaluationPublisher implements TransactionEvaluationPublisher {

	private final EvaluationMessageSender messageSender;

	public RabbitTransactionEvaluationPublisher(EvaluationMessageSender messageSender) {
		this.messageSender = messageSender;
	}

	@Override
	public void publish(Long transactionId) {
		TransactionEvaluationRequested message = new TransactionEvaluationRequested(
				transactionId,
				Instant.now());
		messageSender.send(
				RabbitMqConfig.TXN_EVENTS_EXCHANGE,
				RabbitMqConfig.EVALUATION_ROUTING_KEY,
				message);
	}
}

@Configuration
@ConditionalOnProperty(prefix = "txnmonitor.evaluation", name = "mode", havingValue = "async")
class RabbitEvaluationMessageSenderConfig {

	@Bean
	EvaluationMessageSender evaluationMessageSender(RabbitTemplate rabbitTemplate) {
		return rabbitTemplate::convertAndSend;
	}
}
