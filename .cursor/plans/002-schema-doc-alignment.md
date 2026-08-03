# Plan: Align ERD + database docs with MVP schema decision

## Goal
Fix inconsistent schema docs so agents and humans build the same model:
denormalized `account_id` / `payee_id` on transactions (no Account/Payee
tables), alert↔transaction junction for multi-txn alerts, no rules DB
table for MVP.

## Steps
1. Rewrite `docs/transaction monitoring er diagram.mmd` to MVP entities only
2. Rewrite `docs/DATABASE_DESIGN.md` to match (columns, indexes, relationships)
3. Add a short "Schema decisions" section to `.cursor/rules/backend-java.mdc`
4. Point `Project_milestones.md` architecture reference at the updated ERD

## Decisions locked in
- No `ACCOUNT` / `PAYEE` master tables — IDs live on `transactions`
- MVP tables: `transactions`, `alerts`, `alert_transactions`
- Alert stores `rule_type` (string), not FK to a `rules` table (MVP hardcoded)
- `alert_status_history`, `rules`, `rule_parameters` = future (documented, not built)
- Indexes: `(account_id, timestamp)`, `(account_id, payee_id)` for Phase 2 rules

## Result
Docs and `backend-java.mdc` / milestones architecture note updated to
the denormalized MVP schema. Also aligned `docs/API_ENDPOINT.md` field
names to `accountId` / `payeeId`. No code/migrations yet.
