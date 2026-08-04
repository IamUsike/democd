# Plan: Rule Engine MVP (B-1)

## Goal

Ship Person B’s B-1 milestone: `Rule` / `RuleEngine` / `AmountThresholdRule`
under `com.example.txnmonitor.rule`, TDD’d with pure unit tests, with a clear
handoff contract for Person A’s transaction service — no alert persistence or
record→evaluate wiring yet.

## Locked decisions

| Decision | Choice |
|----------|--------|
| Threshold | Hardcoded `10000` via constructor; injectable for tests — no `rules` table |
| Trigger | `amount.compareTo(threshold) > 0` (equal → no match) |
| Return type | `List<RuleMatch>` (`ruleType`, `severity`, `reason`, `transactionId`) |
| Input | `TransactionSnapshot(id, amount)` until A’s entity merges |
| Context | Empty `RuleEvaluationContext` / `NoOpRuleEvaluationContext` for MVP |

## Steps

1. TDD `AmountThresholdRule` (above / equal / below)
2. TDD `RuleEngine` aggregation
3. `RuleEngineConfig` Spring beans
4. Document evaluate handoff in `rule/package-info.java`
5. Check off rule-engine skeleton in `Project_milestones.md`

## Result

Shipped:

- `Rule`, `RuleMatch`, `TransactionSnapshot`, `RuleEvaluationContext`,
  `NoOpRuleEvaluationContext`, `AmountThresholdRule`, `RuleEngine`,
  `RuleEngineConfig`
- Unit tests: `AmountThresholdRuleTest`, `RuleEngineTest`
- Handoff contract documented on `rule/package-info.java`
- Milestone “Rule engine skeleton” checked; wire-up left unchecked

Not in this PR: AlertService, record→evaluate wiring, HTTP, queue.
