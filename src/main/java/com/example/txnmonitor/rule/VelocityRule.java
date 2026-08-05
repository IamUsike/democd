package com.example.txnmonitor.rule;

import java.util.List;
import java.util.Objects;

/**
 * Fires when an account exceeds a transaction count within a time window.
 */
public final class VelocityRule implements Rule {

	public static final String RULE_TYPE = "VELOCITY";
	public static final String SEVERITY = "MEDIUM";

	private final int maxTransactions;
	private final int windowMinutes;

	public VelocityRule(int maxTransactions, int windowMinutes) {
		if (maxTransactions < 1) {
			throw new IllegalArgumentException("maxTransactions must be positive");
		}
		if (windowMinutes < 1) {
			throw new IllegalArgumentException("windowMinutes must be positive");
		}
		this.maxTransactions = maxTransactions;
		this.windowMinutes = windowMinutes;
	}

	@Override
	public List<RuleMatch> evaluate(TransactionSnapshot txn, RuleEvaluationContext context) {
		Objects.requireNonNull(txn, "txn");
		Objects.requireNonNull(txn.accountId(), "txn.accountId");
		Objects.requireNonNull(txn.timestamp(), "txn.timestamp");
		Objects.requireNonNull(context, "context");

		var since = txn.timestamp().minusMinutes(windowMinutes);
		long count = context.countRecentTransactions(txn.accountId(), since);

		if (count <= maxTransactions) {
			return List.of();
		}

		String reason = "Velocity Rule triggered: "
				+ count
				+ " transactions for account "
				+ txn.accountId()
				+ " within "
				+ windowMinutes
				+ " minutes (max "
				+ maxTransactions
				+ ")";

		return List.of(new RuleMatch(RULE_TYPE, SEVERITY, reason, txn.id()));
	}
}
