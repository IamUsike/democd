package com.example.txnmonitor.rule;

import java.util.List;
import java.util.Objects;

/**
 * Fires when an account exceeds a transaction count within a rolling time window.
 * Parameters (maxTransactions, windowMinutes) are initialised from the DB at startup
 * and can be updated at runtime without a restart via {@link #applyConfig(RuleConfig)}.
 */
public final class VelocityRule implements ConfigurableRule {

	public static final String RULE_TYPE = "VELOCITY";
	public static final String SEVERITY = "MEDIUM";

	private volatile int maxTransactions;
	private volatile int windowMinutes;
	private volatile boolean enabled = true;

	public VelocityRule(int maxTransactions, int windowMinutes) {
		validateParams(maxTransactions, windowMinutes);
		this.maxTransactions = maxTransactions;
		this.windowMinutes = windowMinutes;
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
	 * Applies DB-sourced configuration: enabled flag, max transactions and window.
	 * Safe to call from both startup initialisation and runtime updates.
	 */
	@Override
	public void applyConfig(RuleConfig config) {
		Objects.requireNonNull(config, "config");
		this.enabled = config.isEnabled();
		if (config.getVelocityMaxTransactions() != null && config.getVelocityMaxTransactions() > 0) {
			this.maxTransactions = config.getVelocityMaxTransactions();
		}
		if (config.getVelocityWindowMinutes() != null && config.getVelocityWindowMinutes() > 0) {
			this.windowMinutes = config.getVelocityWindowMinutes();
		}
	}

	// ── Runtime update helpers (kept for backward-compat with existing tests) ─

	/** Called by {@link RuleConfigService} when the operator updates velocity parameters. */
	public void updateConfig(int maxTransactions, int windowMinutes) {
		validateParams(maxTransactions, windowMinutes);
		this.maxTransactions = maxTransactions;
		this.windowMinutes = windowMinutes;
	}

	public int getMaxTransactions() { return maxTransactions; }
	public int getWindowMinutes()   { return windowMinutes; }

	private static void validateParams(int maxTransactions, int windowMinutes) {
		if (maxTransactions < 1) throw new IllegalArgumentException("maxTransactions must be positive");
		if (windowMinutes < 1)   throw new IllegalArgumentException("windowMinutes must be positive");
	}

	// ── Rule ──────────────────────────────────────────────────────────────────

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
