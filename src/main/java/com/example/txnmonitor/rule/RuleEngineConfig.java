package com.example.txnmonitor.rule;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.txnmonitor.common.config.TxnMonitorProperties;
import com.example.txnmonitor.transaction.TransactionRepository;

/**
 * Spring configuration that constructs and seeds rule beans from DB-persisted config.
 *
 * <p>Each rule bean is first created with safe defaults, then immediately seeded via
 * {@link ConfigurableRule#applyConfig(RuleConfig)} so both the threshold values AND the
 * enabled state are consistent with the operator's last-saved configuration on startup.
 */
@Configuration
public class RuleEngineConfig {

	public static final BigDecimal DEFAULT_AMOUNT_THRESHOLD        = new BigDecimal("10000");
	public static final int        DEFAULT_VELOCITY_MAX            = 5;
	public static final int        DEFAULT_VELOCITY_WINDOW_MINUTES = 10;
	public static final BigDecimal DEFAULT_DAILY_LIMIT             = new BigDecimal("50000");

	private final RuleConfigRepository ruleConfigRepository;
	private final TxnMonitorProperties txnMonitorProperties;

	public RuleEngineConfig(RuleConfigRepository ruleConfigRepository, TxnMonitorProperties txnMonitorProperties) {
		this.ruleConfigRepository = ruleConfigRepository;
		this.txnMonitorProperties = txnMonitorProperties;
	}

	@Bean
	AmountThresholdRule amountThresholdRule() {
		AmountThresholdRule rule = new AmountThresholdRule(DEFAULT_AMOUNT_THRESHOLD);
		ruleConfigRepository.findById("AMOUNT_THRESHOLD").ifPresent(rule::applyConfig);
		return rule;
	}

	@Bean
	VelocityRule velocityRule() {
		VelocityRule rule = new VelocityRule(DEFAULT_VELOCITY_MAX, DEFAULT_VELOCITY_WINDOW_MINUTES);
		ruleConfigRepository.findById("VELOCITY").ifPresent(rule::applyConfig);
		return rule;
	}

	@Bean
	NewPayeeRule newPayeeRule() {
		NewPayeeRule rule = new NewPayeeRule();
		ruleConfigRepository.findById("NEW_PAYEE").ifPresent(rule::applyConfig);
		return rule;
	}

	@Bean
	DailyLimitRule dailyLimitRule() {
		DailyLimitRule rule = new DailyLimitRule(DEFAULT_DAILY_LIMIT);
		ruleConfigRepository.findById("DAILY_LIMIT").ifPresent(rule::applyConfig);
		return rule;
	}

	@Bean
	RuleEngine ruleEngine(List<Rule> rules) {
		return new RuleEngine(rules);
	}

	@Bean
	RuleEvaluationContext ruleEvaluationContext(TransactionRepository transactionRepository) {
		RuleEvaluationContext repositoryContext = new RepositoryRuleEvaluationContext(transactionRepository);
		if (txnMonitorProperties.getRuleEvaluation().isEnabled()) {
			return new CachingRuleEvaluationContext(repositoryContext);
		}
		return repositoryContext;
	}
}
