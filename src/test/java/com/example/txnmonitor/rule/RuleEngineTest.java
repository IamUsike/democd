package com.example.txnmonitor.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class RuleEngineTest {

	private static final Long TXN_ID = 7L;
	private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 8, 5, 12, 0);
	private final RuleEvaluationContext context = new NoOpRuleEvaluationContext();

	@Test
	void evaluate_noRulesFire_returnsEmpty() {
		RuleEngine engine = new RuleEngine(List.of(new AmountThresholdRule(new BigDecimal("10000"))));
		TransactionSnapshot txn = snapshot(new BigDecimal("100"));

		List<RuleMatch> matches = engine.evaluate(txn, context);

		assertTrue(matches.isEmpty());
	}

	@Test
	void evaluate_amountRuleFires_returnsItsMatch() {
		RuleEngine engine = new RuleEngine(List.of(new AmountThresholdRule(new BigDecimal("10000"))));
		TransactionSnapshot txn = snapshot(new BigDecimal("15000"));

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
		TransactionSnapshot txn = snapshot(new BigDecimal("20000"));

		List<RuleMatch> matches = engine.evaluate(txn, context);

		assertEquals(2, matches.size());
		assertEquals("AMOUNT_THRESHOLD", matches.get(0).ruleType());
		assertEquals("STUB_RULE", matches.get(1).ruleType());
	}

	@Test
	void evaluate_emptyRuleList_returnsEmpty() {
		RuleEngine engine = new RuleEngine(List.of());
		TransactionSnapshot txn = snapshot(new BigDecimal("99999"));

		assertTrue(engine.evaluate(txn, context).isEmpty());
	}

	@Test
	void evaluate_disabledConfigurableRule_isSkipped() {
		AmountThresholdRule rule = new AmountThresholdRule(new BigDecimal("10000"));
		RuleConfig disabled = new RuleConfig();
		disabled.setRuleType(AmountThresholdRule.RULE_TYPE);
		disabled.setEnabled(false);
		disabled.setAmountThreshold(new BigDecimal("10000"));
		rule.applyConfig(disabled);

		RuleEngine engine = new RuleEngine(List.of(rule));
		TransactionSnapshot txn = snapshot(new BigDecimal("20000"));

		assertTrue(engine.evaluate(txn, context).isEmpty());
	}

	@Test
	void evaluate_enabledConfigurableRule_stillFires() {
		AmountThresholdRule rule = new AmountThresholdRule(new BigDecimal("10000"));
		RuleEngine engine = new RuleEngine(List.of(rule));

		List<RuleMatch> matches = engine.evaluate(snapshot(new BigDecimal("20000")), context);

		assertEquals(1, matches.size());
		assertEquals("AMOUNT_THRESHOLD", matches.get(0).ruleType());
	}

	private static TransactionSnapshot snapshot(BigDecimal amount) {
		return new TransactionSnapshot(TXN_ID, amount, "ACC-1", "PAYEE-1", TIMESTAMP, "DEBIT");
	}
}
