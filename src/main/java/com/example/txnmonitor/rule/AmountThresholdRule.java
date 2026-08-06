package com.example.txnmonitor.rule;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Fires when a transaction amount strictly exceeds the configured threshold.
 * Threshold is initialised from the DB at startup and can be updated at runtime
 * without a restart via {@link #applyConfig(RuleConfig)}.
 */
public final class AmountThresholdRule implements ConfigurableRule {

	public static final String RULE_TYPE = "AMOUNT_THRESHOLD";
	public static final String SEVERITY = "MEDIUM";

	private volatile BigDecimal threshold;
	private volatile boolean enabled = true;

	public AmountThresholdRule(BigDecimal threshold) {
		this.threshold = Objects.requireNonNull(threshold, "threshold");
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
	 * Applies DB-sourced configuration: enabled flag and threshold value.
	 * Safe to call from both startup initialisation and runtime updates.
	 */
	@Override
	public void applyConfig(RuleConfig config) {
		Objects.requireNonNull(config, "config");
		this.enabled = config.isEnabled();
		if (config.getAmountThreshold() != null
				&& config.getAmountThreshold().compareTo(BigDecimal.ZERO) > 0) {
			this.threshold = config.getAmountThreshold();
		}
	}

	// ── Runtime update helpers (kept for backward-compat with existing tests) ─

	/** Called by {@link RuleConfigService} when the operator updates the threshold. */
	public void updateThreshold(BigDecimal threshold) {
		this.threshold = Objects.requireNonNull(threshold, "threshold");
	}

	public BigDecimal getThreshold() {
		return threshold;
	}

	// ── Rule ──────────────────────────────────────────────────────────────────

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
