package com.example.txnmonitor.rule;

/**
 * Supplies extra data rules may need beyond the single transaction
 * (velocity counts, daily sums, etc.). Empty for MVP — Amount Threshold
 * only needs {@link TransactionSnapshot#amount()}.
 */
public interface RuleEvaluationContext {
}
