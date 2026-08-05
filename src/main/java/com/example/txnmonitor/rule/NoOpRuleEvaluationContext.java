package com.example.txnmonitor.rule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * No-op context for unit tests that do not need repository lookups.
 */
public final class NoOpRuleEvaluationContext implements RuleEvaluationContext {

	@Override
	public long countRecentTransactions(String accountId, LocalDateTime since) {
		return 0L;
	}

	@Override
	public long countPriorPayeeTransactions(String accountId, String payeeId, Long excludeTransactionId) {
		return 0L;
	}

	@Override
	public BigDecimal sumDebitAmountOnDate(String accountId, LocalDate date) {
		return BigDecimal.ZERO;
	}
}
