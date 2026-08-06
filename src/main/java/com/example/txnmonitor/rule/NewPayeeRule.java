package com.example.txnmonitor.rule;

import java.util.List;
import java.util.Objects;

/**
 * Fires when a transaction is the first from an account to a payee.
 * This rule has no numeric parameters — it can only be enabled or disabled
 * via {@link #applyConfig(RuleConfig)}.
 */
public final class NewPayeeRule implements ConfigurableRule {

	public static final String RULE_TYPE = "NEW_PAYEE";
	public static final String SEVERITY = "MEDIUM";

	private volatile boolean enabled = true;

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
	 * Applies DB-sourced configuration: enabled flag only (no numeric parameters).
	 * Safe to call from both startup initialisation and runtime updates.
	 */
	@Override
	public void applyConfig(RuleConfig config) {
		Objects.requireNonNull(config, "config");
		this.enabled = config.isEnabled();
	}

	// ── Rule ──────────────────────────────────────────────────────────────────

	@Override
	public List<RuleMatch> evaluate(TransactionSnapshot txn, RuleEvaluationContext context) {
		Objects.requireNonNull(txn, "txn");
		Objects.requireNonNull(txn.accountId(), "txn.accountId");
		Objects.requireNonNull(txn.payeeId(), "txn.payeeId");
		Objects.requireNonNull(txn.id(), "txn.id");
		Objects.requireNonNull(context, "context");

		long priorCount = context.countPriorPayeeTransactions(txn.accountId(), txn.payeeId(), txn.id());

		if (priorCount > 0) {
			return List.of();
		}

		String reason = "New Payee Rule triggered: first transaction from account "
				+ txn.accountId()
				+ " to payee "
				+ txn.payeeId();

		return List.of(new RuleMatch(RULE_TYPE, SEVERITY, reason, txn.id()));
	}
}
