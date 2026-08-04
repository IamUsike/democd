# Project Milestones — Transaction Monitoring & Alerts

Update this file as milestones complete. Cursor agents read this before
starting work — keep "Current status" honest and current, not aspirational.

## Current status
**Phase:** Phase 1 — MVP in progress
**Last updated:** 04 August 2026
**Currently working on:** Next — Transaction entity + repository
(enriched multi-source fields)

---

## Phase 1 — MVP

Build in this order. Don't start an item until the one above it works and
is tested.

Soft tenancy: every transaction carries `source_type` (`BANK`|`MERCHANT`),
`source_id`, and `source_name`. Feeds are simulated via the public ingest
API. Rules are **hardcoded** (Amount Threshold). MTTD goal: synchronous
evaluate-on-record.

- [x] Project skeleton — Spring Boot app, package structure per
      `backend-java.mdc`, migration tool configured, git repo pushed
- [ ] Transaction entity + repository (fields: id, source_type, source_id,
      source_name, account_id, payee_id, payee_name, amount, currency,
      type, timestamp, location, latitude, longitude, description, status)
- [ ] `POST /transactions`, `GET /transactions` (list + filter/search by
      source and account) — **public simulate/ingest** for bank & merchant
      sims uses this same contract
- [ ] Rule engine skeleton — `Rule` interface, `RuleEngine`,
      `AmountThresholdRule` (single hardcoded threshold)
- [ ] Wire transaction recording → synchronous rule evaluation
- [ ] Alert entity + repository (status, severity, rule reference,
      source_type/source_id/source_name denormalized, account_id,
      created/acknowledged/closed timestamps, resolution notes,
      `alert_transactions`)
- [ ] Alert lifecycle endpoints — acknowledge, investigate, close,
      dismiss — with transition validation
- [ ] `GET /alerts`, `GET /alerts/{id}` (with triggering transactions;
      filter by source)
- [ ] React UI: bluish theme; transaction list (filter/search by source)
- [ ] React UI: alert list + alert detail + lifecycle action buttons
- [ ] Basic dashboard KPI strip (txn counts by source, open alerts —
      aggregations, not stored columns)
- [ ] Swagger/OpenAPI docs generated
- [ ] Seed / simulate script hitting `POST /transactions` for sample
      BANK and MERCHANT sources
- [ ] End-to-end MVP demo: post a transaction over threshold from a
      simulated bank/merchant, see the alert appear, acknowledge and
      close it, from the UI

## Phase 2 — additional rule types + user-configurable rules

- [ ] `VelocityRule` (N transactions within T minutes)
- [ ] `NewPayeeRule` (first transaction to an unseen payee)
- [ ] `DailyLimitRule` (cumulative daily amount)
- [ ] Severity levels (HIGH/MEDIUM/LOW) surfaced in UI with color coding
- [ ] Rules table / parameters + management UI (view; create/edit so
      operators can set thresholds — stretch to full authoring)
- [ ] Additional team-authored custom rules as needed for the demo

## Phase 3 — scale-out (MTTD at volume)

- [ ] Message queue between transaction recording and rule evaluation
- [ ] Rule engine extracted as its own deployable, calling back into the
      monolith's internal alert-creation endpoint
- [ ] Horizontal scaling of rule engine instances demonstrated
- [ ] Cache for velocity/daily-limit count lookups
- [ ] Read replica for reporting/dashboard KPI queries

## Phase 4 — security & hardening

- [ ] TLS confirmed end-to-end (client↔API at minimum)
- [ ] Database encryption at rest enabled
- [ ] Mask sensitive fields in API/UI where practical (account, payee,
      location) — show full values only when needed for investigation
- [ ] Credentials out of source (env vars minimum; secrets manager if
      time allows)
- [ ] Audit trail confirmed complete (who/what/when for every alert
      status change)

## Explicitly out of scope
- Machine learning / anomaly detection — not doing this; would eat the
  remaining time budget for low presentation payoff relative to a
  well-scaled, secured system.
- Authentication / multi-operator support — spec says single operator,
  no auth required.
- Hard tenancy (separate DB/schema per bank) — soft tenancy only.
- Live bank/merchant network integrations — simulate via public API.

## Architecture reference
See `.cursor/rules/general.mdc` for the modular monolith decision and
build-order rationale. Schema: `docs/DATABASE_DESIGN.md` +
`docs/transaction monitoring er diagram.mmd` — soft multi-source
tenancy; `source_*` / `account_id` / `payee_id` on transactions; MVP
tables `transactions`, `alerts`, `alert_transactions`. Flows:
[`docs/DFD-MVP.md`](docs/DFD-MVP.md). Three-person ownership:
[`docs/TEAM_WORK_SPLIT.md`](docs/TEAM_WORK_SPLIT.md). HLD/LLD diagrams live
wherever the team has put them (Miro/Figma/Drive link — *add link here*).
