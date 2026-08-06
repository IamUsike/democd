# Project Milestones — Transaction Monitoring & Alerts

Update this file as milestones complete. Cursor agents read this before
starting work — keep "Current status" honest and current, not aspirational.

## Current status

**Phase:** Phase 3 — async queue + cache **in progress** (worker extract + read replica next)  
**Last updated:** 06 August 2026  
**Currently working on:**  
- **shreya (A):** E2E / k6 load testing (QA-1), results write-up  
- **sathwik (B):** login + superadmin; Actuator/deploy support *(slightly broader B + infra)*  
- **Rameez (C):** severity colors + alert status filter/sort (client Ready)

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
- [x] Transaction entity + repository (fields: id, source_type, source_id,
  source_name, account_id, payee_id, payee_name, amount, currency,
  type, timestamp, location, latitude, longitude, description, status)
- [x] `POST /transactions`, `GET /transactions` (list + filter/search by
  source and account) — **public simulate/ingest** for bank & merchant
  sims uses this same contract
- [x] Rule engine skeleton — `Rule` interface, `RuleEngine`,
  `AmountThresholdRule` (single hardcoded threshold)
- [x] Wire transaction recording → synchronous rule evaluation
- [x] Alert entity + repository (status, severity, rule reference,
  source_type/source_id/source_name denormalized, account_id,
  created/acknowledged/closed timestamps, resolution notes,
  `alert_transactions`)
- [x] Alert lifecycle endpoints — acknowledge, investigate, close,
  dismiss — with transition validation
- [x] `GET /alerts`, `GET /alerts/{id}` (with triggering transactions;
      filter by source)
- [x] React UI: Dark Obsidian theme; zero horizontal scroll; transaction list (filter/search by source)
- [x] React UI: alert list + alert detail + lifecycle action buttons
- [x] Basic dashboard KPI strip (txn counts by source, open alerts —
      aggregations, not stored columns)
- [x] Swagger/OpenAPI docs generated
- [x] Seed / simulate script hitting `POST /transactions` for sample
  BANK and MERCHANT sources
- [x] End-to-end MVP path ready: seed over-threshold → OPEN alert →
  acknowledge/close from UI (mark live room demo done when presented;
  runbook in `docs/MVP_PRESENTATION.md`)



## Phase 2 — additional rule types + user-configurable rules

- [x] `VelocityRule` (N transactions within T minutes)
- [x] `NewPayeeRule` (first transaction to an unseen payee)
- [x] `DailyLimitRule` (cumulative daily amount)
- [ ] Severity levels (HIGH/MEDIUM/LOW) surfaced in UI with color coding
- [ ] Rules table / parameters + management UI (view; create/edit so
  operators can set thresholds — stretch to full authoring)
- [ ] Rule explanations in UI (what each rule checks)
- [ ] Failing reason on alerts (why the rule matched) visible in list/detail
- [ ] Alert list filter + sort by status in UI
- [ ] Dashboard graphs (trends / counts over time)
- [ ] Additional team-authored custom rules as needed for the demo
- [ ] QA: E2E + load test write-up (`docs/LOAD_TEST_GUIDE.md` / `load-test-results.md`)
- [ ] Login page + superadmin (single-operator gate)


## Phase 3 — scale-out (MTTD at volume)

- [x] Message queue between transaction recording and rule evaluation (RabbitMQ; `txnmonitor.evaluation.mode=async`)
- [x] Cache for velocity/daily-limit count lookups (`txnmonitor.rule-evaluation.cache.enabled`)
- [x] Internal alert-creation endpoint (`POST /internal/alerts`) for future worker extract
- [x] Dedicated DB VM compose overlay (`docker-compose.prod.yml` — app + RabbitMQ; external `DB_URL`)
- [ ] Rule engine extracted as its own deployable, calling back into the
  monolith's internal alert-creation endpoint
- [ ] Horizontal scaling of rule engine instances demonstrated
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
- ~~Authentication / multi-operator support~~ — single-operator login
  gate added (hardcoded superadmin `admin`/`password`); full multi-user
  auth remains out of scope.
- Hard tenancy (separate DB/schema per bank) — soft tenancy only.
- Live bank/merchant network integrations — simulate via public API.

## Architecture reference
See `.cursor/rules/general.mdc` for the modular monolith decision and
build-order rationale. Schema: `docs/DATABASE_DESIGN.md` +
`docs/transaction monitoring er diagram.mmd` — soft multi-source
tenancy; `source_*` / `account_id` / `payee_id` on transactions; MVP
tables `transactions`, `alerts`, `alert_transactions`. Flows:
[`docs/DFD-MVP.md`](docs/DFD-MVP.md). Three-person ownership:
[`docs/TEAM_WORK_SPLIT.md`](docs/TEAM_WORK_SPLIT.md). Storyline:
[`docs/STORYLINE_AND_KANBAN.md`](docs/STORYLINE_AND_KANBAN.md). User stories (TMD backlog):
[`docs/USER_STORIES.md`](docs/USER_STORIES.md). Live board:
[`docs/KANBAN.md`](docs/KANBAN.md). Stand-ups:
[`docs/STANDUP_LOG.md`](docs/STANDUP_LOG.md). Client meetings:
[`docs/MEETING_NOTES.md`](docs/MEETING_NOTES.md). Retrospectives:
[`docs/SPRINT_RETROSPECTIVE.md`](docs/SPRINT_RETROSPECTIVE.md). HLD/LLD diagrams live
wherever the team has put them (Miro/Figma/Drive link — *add link here*).
