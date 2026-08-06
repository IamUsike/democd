package com.example.txnmonitor.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Runs all registered {@link Rule}s against a transaction and aggregates matches.
 * Does not know about concrete rule types.
 *
 * <p>Rules that implement {@link ConfigurableRule} and are currently disabled
 * are silently skipped — their {@code evaluate()} method is never called.
 */
public final class RuleEngine {

	private final List<Rule> rules;

	public RuleEngine(List<Rule> rules) {
		this.rules = List.copyOf(Objects.requireNonNull(rules, "rules"));
	}

	public List<RuleMatch> evaluate(TransactionSnapshot txn, RuleEvaluationContext context) {
		Objects.requireNonNull(txn, "txn");
		Objects.requireNonNull(context, "context");

		List<RuleMatch> matches = new ArrayList<>();
		for (Rule rule : rules) {
			// Skip disabled configurable rules — honours the operator's enabled toggle
			if (rule instanceof ConfigurableRule cr && !cr.isEnabled()) {
				continue;
			}
			matches.addAll(rule.evaluate(txn, context));
		}
		return List.copyOf(matches);
	}
}
