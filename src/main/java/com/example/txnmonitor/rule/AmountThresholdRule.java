package com.example.txnmonitor.rule;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Fires when a transaction amount strictly exceeds a hardcoded threshold.
 */
public final class AmountThresholdRule implements Rule {

	public static final String RULE_TYPE = "AMOUNT_THRESHOLD";
	public static final String SEVERITY = "HIGH";

	private final BigDecimal threshold;

	public AmountThresholdRule(BigDecimal threshold) {
		this.threshold = Objects.requireNonNull(threshold, "threshold");
	}

	@Override
	public List<RuleMatch> evaluate(TransactionSnapshot txn, RuleEvaluationContext context) {
		Objects.requireNonNull(txn, "txn");
		Objects.requireNonNull(txn.amount(), "txn.amount");

		if (txn.amount().compareTo(threshold) <= 0) {
			return List.of();
		}

		String reason = "Amount Threshold Rule triggered: amount "
				+ txn.amount()
				+ " exceeds threshold "
				+ threshold;

		return List.of(new RuleMatch(RULE_TYPE, SEVERITY, reason, txn.id()));
	}
}
