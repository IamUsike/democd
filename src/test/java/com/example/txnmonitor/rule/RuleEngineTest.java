package com.example.txnmonitor.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class RuleEngineTest {

	private static final Long TXN_ID = 7L;
	private final RuleEvaluationContext context = new NoOpRuleEvaluationContext();

	@Test
	void evaluate_noRulesFire_returnsEmpty() {
		RuleEngine engine = new RuleEngine(List.of(new AmountThresholdRule(new BigDecimal("10000"))));
		TransactionSnapshot txn = new TransactionSnapshot(TXN_ID, new BigDecimal("100"));

		List<RuleMatch> matches = engine.evaluate(txn, context);

		assertTrue(matches.isEmpty());
	}

	@Test
	void evaluate_amountRuleFires_returnsItsMatch() {
		RuleEngine engine = new RuleEngine(List.of(new AmountThresholdRule(new BigDecimal("10000"))));
		TransactionSnapshot txn = new TransactionSnapshot(TXN_ID, new BigDecimal("15000"));

		List<RuleMatch> matches = engine.evaluate(txn, context);

		assertEquals(1, matches.size());
		assertEquals("AMOUNT_THRESHOLD", matches.get(0).ruleType());
		assertEquals(TXN_ID, matches.get(0).transactionId());
	}

	@Test
	void evaluate_multipleRules_aggregatesAllMatches() {
		Rule alwaysFires = (txn, ctx) -> List.of(
				new RuleMatch("STUB_RULE", "LOW", "stub fired", txn.id()));
		RuleEngine engine = new RuleEngine(List.of(
				new AmountThresholdRule(new BigDecimal("10000")),
				alwaysFires));
		TransactionSnapshot txn = new TransactionSnapshot(TXN_ID, new BigDecimal("20000"));

		List<RuleMatch> matches = engine.evaluate(txn, context);

		assertEquals(2, matches.size());
		assertEquals("AMOUNT_THRESHOLD", matches.get(0).ruleType());
		assertEquals("STUB_RULE", matches.get(1).ruleType());
	}

	@Test
	void evaluate_emptyRuleList_returnsEmpty() {
		RuleEngine engine = new RuleEngine(List.of());
		TransactionSnapshot txn = new TransactionSnapshot(TXN_ID, new BigDecimal("99999"));

		assertTrue(engine.evaluate(txn, context).isEmpty());
	}
}
