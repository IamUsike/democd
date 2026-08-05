# 007 — Phase 2 Rules (Velocity, New Payee, Daily Limit)

Branch: `feature/phase2-rules` from `dev`

## Goal
Add three synchronous rules behind existing `Rule` / `RuleEngine` contract.
Extend `TransactionSnapshot` and `RuleEvaluationContext` with repository lookups.

## Rules
| Rule | Trigger | Constants |
|------|---------|-----------|
| Velocity | count > 5 in 10 min per account | max=5, window=10 min |
| New Payee | no prior txn for account+payee | exclude current id |
| Daily Limit | DEBIT sum > 50000 on txn day | limit=50000 |

## Out of scope
Queue, rules table/UI, frontend severity colors, caching.
