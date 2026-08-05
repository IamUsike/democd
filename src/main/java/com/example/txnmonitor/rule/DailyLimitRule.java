package com.example.txnmonitor.rule;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Fires when cumulative DEBIT amount for an account on a calendar day exceeds a limit.
 */
public final class DailyLimitRule implements Rule {

	public static final String RULE_TYPE = "DAILY_LIMIT";
	public static final String SEVERITY = "HIGH";
	private static final String DEBIT_TYPE = "DEBIT";

	private final BigDecimal dailyLimit;

	public DailyLimitRule(BigDecimal dailyLimit) {
		this.dailyLimit = Objects.requireNonNull(dailyLimit, "dailyLimit");
		if (dailyLimit.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("dailyLimit must be positive");
		}
	}

	@Override
	public List<RuleMatch> evaluate(TransactionSnapshot txn, RuleEvaluationContext context) {
		Objects.requireNonNull(txn, "txn");
		Objects.requireNonNull(txn.accountId(), "txn.accountId");
		Objects.requireNonNull(txn.timestamp(), "txn.timestamp");
		Objects.requireNonNull(txn.type(), "txn.type");
		Objects.requireNonNull(context, "context");

		if (!DEBIT_TYPE.equals(txn.type())) {
			return List.of();
		}

		BigDecimal dailyTotal = context.sumDebitAmountOnDate(txn.accountId(), txn.timestamp().toLocalDate());

		if (dailyTotal.compareTo(dailyLimit) <= 0) {
			return List.of();
		}

		String reason = "Daily Limit Rule triggered: DEBIT total "
				+ dailyTotal
				+ " for account "
				+ txn.accountId()
				+ " on "
				+ txn.timestamp().toLocalDate()
				+ " exceeds limit "
				+ dailyLimit;

		return List.of(new RuleMatch(RULE_TYPE, SEVERITY, reason, txn.id()));
	}
}
