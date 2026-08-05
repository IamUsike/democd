package com.example.txnmonitor.rule;

import java.util.List;
import java.util.Objects;

/**
 * Fires when a transaction is the first from an account to a payee.
 */
public final class NewPayeeRule implements Rule {

	public static final String RULE_TYPE = "NEW_PAYEE";
	public static final String SEVERITY = "MEDIUM";

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
