/**
 * Rule engine — {@link com.example.txnmonitor.rule.Rule},
 * {@link com.example.txnmonitor.rule.RuleEngine}, and rule implementations.
 *
 * <h2>Handoff for TransactionService (Person A / B-2 wire-up)</h2>
 * After a transaction is persisted, call:
 * <pre>
 *   TransactionSnapshot snapshot = new TransactionSnapshot(saved.getId(), saved.getAmount());
 *   List&lt;RuleMatch&gt; matches = ruleEngine.evaluate(snapshot, ruleEvaluationContext);
 * </pre>
 * If {@code matches} is non-empty, AlertService creates OPEN alerts (Person B,
 * next milestone). Do not put rule-type if/else in TransactionService.
 */
package com.example.txnmonitor.rule;
