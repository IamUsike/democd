package com.example.txnmonitor.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AmountThresholdRuleTest {

	private static final BigDecimal THRESHOLD = new BigDecimal("10000");
	private static final Long TXN_ID = 42L;

	private AmountThresholdRule rule;
	private RuleEvaluationContext context;

	@BeforeEach
	void setUp() {
		rule = new AmountThresholdRule(THRESHOLD);
		context = new NoOpRuleEvaluationContext();
	}

	@Test
	void evaluate_amountAboveThreshold_returnsMatch() {
		TransactionSnapshot txn = new TransactionSnapshot(TXN_ID, new BigDecimal("10000.01"));

		List<RuleMatch> matches = rule.evaluate(txn, context);

		assertEquals(1, matches.size());
		RuleMatch match = matches.get(0);
		assertEquals("AMOUNT_THRESHOLD", match.ruleType());
		assertEquals("HIGH", match.severity());
		assertEquals(TXN_ID, match.transactionId());
		assertTrue(match.reason().contains("10000"));
	}

	@Test
	void evaluate_amountBelowThreshold_returnsEmpty() {
		TransactionSnapshot txn = new TransactionSnapshot(TXN_ID, new BigDecimal("9999.99"));

		List<RuleMatch> matches = rule.evaluate(txn, context);

		assertTrue(matches.isEmpty());
	}

	@Test
	void evaluate_amountEqualToThreshold_returnsEmpty() {
		TransactionSnapshot txn = new TransactionSnapshot(TXN_ID, new BigDecimal("10000"));

		List<RuleMatch> matches = rule.evaluate(txn, context);

		assertTrue(matches.isEmpty());
	}
}
