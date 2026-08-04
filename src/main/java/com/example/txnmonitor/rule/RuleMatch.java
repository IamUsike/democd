package com.example.txnmonitor.rule;

/**
 * Immutable result of a rule firing. AlertService will map matches to
 * persisted OPEN alerts in a later milestone.
 */
public record RuleMatch(
		String ruleType,
		String severity,
		String reason,
		Long transactionId) {
}
