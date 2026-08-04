package com.example.txnmonitor.rule;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RuleEngineConfig {

	public static final BigDecimal DEFAULT_AMOUNT_THRESHOLD = new BigDecimal("10000");

	@Bean
	AmountThresholdRule amountThresholdRule() {
		return new AmountThresholdRule(DEFAULT_AMOUNT_THRESHOLD);
	}

	@Bean
	RuleEngine ruleEngine(List<Rule> rules) {
		return new RuleEngine(rules);
	}

	@Bean
	RuleEvaluationContext ruleEvaluationContext() {
		return new NoOpRuleEvaluationContext();
	}
}
