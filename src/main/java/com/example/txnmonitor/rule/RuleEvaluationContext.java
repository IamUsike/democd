package com.example.txnmonitor.rule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Supplies extra data rules may need beyond the single transaction
 * (velocity counts, daily sums, prior payee history).
 */
public interface RuleEvaluationContext {

	long countRecentTransactions(String accountId, LocalDateTime since);

	long countPriorPayeeTransactions(String accountId, String payeeId, Long excludeTransactionId);

	BigDecimal sumDebitAmountOnDate(String accountId, LocalDate date);
}
