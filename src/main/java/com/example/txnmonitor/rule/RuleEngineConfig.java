package com.example.txnmonitor.rule;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.txnmonitor.transaction.TransactionRepository;

@Configuration
public class RuleEngineConfig {

	public static final BigDecimal DEFAULT_AMOUNT_THRESHOLD = new BigDecimal("10000");
	public static final int DEFAULT_VELOCITY_MAX = 5;
	public static final int DEFAULT_VELOCITY_WINDOW_MINUTES = 10;
	public static final BigDecimal DEFAULT_DAILY_LIMIT = new BigDecimal("50000");

	@Bean
	AmountThresholdRule amountThresholdRule() {
		return new AmountThresholdRule(DEFAULT_AMOUNT_THRESHOLD);
	}

	@Bean
	VelocityRule velocityRule() {
		return new VelocityRule(DEFAULT_VELOCITY_MAX, DEFAULT_VELOCITY_WINDOW_MINUTES);
	}

	@Bean
	NewPayeeRule newPayeeRule() {
		return new NewPayeeRule();
	}

	@Bean
	DailyLimitRule dailyLimitRule() {
		return new DailyLimitRule(DEFAULT_DAILY_LIMIT);
	}

	@Bean
	RuleEngine ruleEngine(List<Rule> rules) {
		return new RuleEngine(rules);
	}

	@Bean
	RuleEvaluationContext ruleEvaluationContext(TransactionRepository transactionRepository) {
		return new RepositoryRuleEvaluationContext(transactionRepository);
	}
}
