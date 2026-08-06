package com.example.txnmonitor.rule;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Fires when cumulative DEBIT amount for an account on a calendar day exceeds the configured limit.
 * The daily limit is initialised from the DB at startup and can be updated at runtime
 * without a restart via {@link #applyConfig(RuleConfig)}.
 */
public final class DailyLimitRule implements ConfigurableRule {

	public static final String RULE_TYPE = "DAILY_LIMIT";
	public static final String SEVERITY = "MEDIUM";
	private static final String DEBIT_TYPE = "DEBIT";

	private volatile BigDecimal dailyLimit;
	private volatile boolean enabled = true;

	public DailyLimitRule(BigDecimal dailyLimit) {
		this.dailyLimit = validated(dailyLimit);
	}

	// ── ConfigurableRule ─────────────────────────────────────────────────────

	@Override
	public String ruleType() {
		return RULE_TYPE;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Applies DB-sourced configuration: enabled flag and daily limit value.
	 * Safe to call from both startup initialisation and runtime updates.
	 */
	@Override
	public void applyConfig(RuleConfig config) {
		Objects.requireNonNull(config, "config");
		this.enabled = config.isEnabled();
		if (config.getDailyLimit() != null && config.getDailyLimit().compareTo(BigDecimal.ZERO) > 0) {
			this.dailyLimit = config.getDailyLimit();
		}
	}

	// ── Runtime update helpers (kept for backward-compat with existing tests) ─

	/** Called by {@link RuleConfigService} when the operator updates the daily limit. */
	public void updateLimit(BigDecimal dailyLimit) {
		this.dailyLimit = validated(dailyLimit);
	}

	public BigDecimal getDailyLimit() { return dailyLimit; }

	private static BigDecimal validated(BigDecimal limit) {
		Objects.requireNonNull(limit, "dailyLimit");
		if (limit.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("dailyLimit must be positive");
		}
		return limit;
	}

	// ── Rule ──────────────────────────────────────────────────────────────────

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
