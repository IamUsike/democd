package com.example.txnmonitor.rule;

import java.math.BigDecimal;

/**
 * Minimal transaction view for rule evaluation. Converge on Person A's
 * Transaction entity once it merges; map saved rows into this snapshot at
 * the RuleEngine call site.
 */
public record TransactionSnapshot(Long id, BigDecimal amount) {
}
