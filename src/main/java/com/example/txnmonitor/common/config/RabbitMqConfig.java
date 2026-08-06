package com.example.txnmonitor.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "txnmonitor.evaluation", name = "mode", havingValue = "async")
public class RabbitMqConfig {

	public static final String TXN_EVENTS_EXCHANGE = "txn.events";
	public static final String EVALUATION_QUEUE = "txn.evaluation";
	public static final String EVALUATION_DLQ = "txn.evaluation.dlq";
	public static final String EVALUATION_ROUTING_KEY = "evaluation.requested";

	@Bean
	DirectExchange txnEventsExchange() {
		return new DirectExchange(TXN_EVENTS_EXCHANGE, true, false);
	}

	@Bean
	Queue evaluationDeadLetterQueue() {
		return QueueBuilder.durable(EVALUATION_DLQ).build();
	}

	@Bean
	Queue evaluationQueue() {
		return QueueBuilder.durable(EVALUATION_QUEUE)
				.withArgument("x-dead-letter-exchange", "")
				.withArgument("x-dead-letter-routing-key", EVALUATION_DLQ)
				.build();
	}

	@Bean
	Binding evaluationBinding(Queue evaluationQueue, DirectExchange txnEventsExchange) {
		return BindingBuilder.bind(evaluationQueue)
				.to(txnEventsExchange)
				.with(EVALUATION_ROUTING_KEY);
	}

	@Bean
	MessageConverter rabbitMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}
}
