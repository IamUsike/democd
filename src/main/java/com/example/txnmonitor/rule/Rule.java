package com.example.txnmonitor.rule;

import java.util.List;

/**
 * Strategy contract for a single monitoring rule. Adding a rule type means
 * a new implementing class — do not change this interface or {@link RuleEngine}.
 */
public interface Rule {

	List<RuleMatch> evaluate(TransactionSnapshot txn, RuleEvaluationContext context);
}
