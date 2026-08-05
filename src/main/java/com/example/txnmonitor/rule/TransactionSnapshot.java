package com.example.txnmonitor.rule;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Minimal transaction view for rule evaluation. Map saved rows into this snapshot
 * at the RuleEngine call site.
 */
public record TransactionSnapshot(
		Long id,
		BigDecimal amount,
		String accountId,
		String payeeId,
		LocalDateTime timestamp,
		String type) {
}
