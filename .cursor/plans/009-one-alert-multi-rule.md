# One alert per transaction (multi-rule)

## Problem

`AlertService.createFromMatches` still creates **one row per rule match**. A
partial change already escalates each of those rows to `HIGH` and appends a
text note — so operators still see N alerts for one txn, which is what
inflated KPI totals in the P3 load run.

## Target behavior

- **1 transaction → at most 1 alert** (skip create if an alert already linked
  to that `transaction_id`).
- **Severity policy:** if `matches.size() > 1` → `HIGH`; else use that
  match’s severity.
- **Rules shown on the alert:** all matched rule types, display names,
  descriptions, and failing reasons — not a truncated single-rule row.

## Backend

- Replace the per-match loop with a single persist in `AlertService`.
- Aggregate `ruleType` (comma-joined sorted), severity, descriptions, and
  failing reasons (` | ` delimiter).
- Add `ruleTypes: List<String>` on `AlertResponse`.
- Dedup via `existsByTransactionId` on `alert_transactions`.
- Flyway `V5__widen_alert_rule_fields.sql`.

## Frontend

- Optional `ruleTypes` on alert type.
- List/detail show multiple broken rules and split reasons/descriptions.

## Out of scope

- Migrating/merging existing duplicate historical alerts.
- Changing individual rule class severities.
- Phase 3 queue/worker extract.
