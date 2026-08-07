# Project Milestones — Transaction Monitoring & Alerts

Update this file as milestones complete. Cursor agents read this before
starting work — keep "Current status" honest and current, not aspirational.

## Current status

**Phase:** Sprint 2 wrap — client UI asks + load evidence **largely done**; Phase 3 infra in progress; **alert/traffic simulator shipped**  
**Last updated:** 07 August 2026  
**Currently working on:**  
- **shreya (A):** simulator quiet traffic; rule_configs empty-UI seeder ✅  
- **sathwik (B):** queue + DB VM migration; confirm login gate  
- **Rameez (C):** presentation dry-run; any residual UI polish  

**Closed today (checklist):** interactive dashboard · rules edit/explain · alert status filter/sort · failing reason · severity · E2E/benchmarks write-up · pagination/`afterId` · virtual alert scroll · multi-rule alerts · search fixes · UI theme · “Mark suspicious” dropped  

**Talk track:** [`FINAL-PRESENTATION.md`](FINAL-PRESENTATION.md) (10 min final) · [`docs/PRESENTATION_SCRIPT.md`](docs/PRESENTATION_SCRIPT.md) · **Load numbers:** [`docs/load-test-results.md`](docs/load-test-results.md)

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
- [x] Server-side pagination + filters/sort on `GET /alerts` and
      `GET /transactions`; alerts default to active statuses; transaction
      delta poll via `afterId`; virtualized lists + debounced search in UI
      (plan: `.cursor/plans/008-list-pagination-ui-smoothness.md`)



## Phase 2 — additional rule types + user-configurable rules

- [x] `VelocityRule` (N transactions within T minutes)
- [x] `NewPayeeRule` (first transaction to an unseen payee)
- [x] `DailyLimitRule` (cumulative daily amount)
- [x] Severity levels (HIGH/MEDIUM/LOW) surfaced in UI with color coding
- [x] Rules table / parameters + management UI (view; create/edit so
  operators can set thresholds — stretch to full authoring)
- [x] Rule explanations in UI (what each rule checks)
- [x] Failing reason on alerts (why the rule matched) visible in list/detail
- [x] Alert list filter + sort by status in UI
- [x] Dashboard graphs (trends / counts over time)
- [ ] Additional team-authored custom rules as needed for the demo
- [x] QA: E2E + load test write-up (`docs/LOAD_TEST_GUIDE.md` / `docs/load-test-results.md`)
- [ ] Login page + superadmin (single-operator gate)
- [x] Alert / traffic simulator for live demo (shreya)  
  — standalone Go+React under `transaction-simulator/`; scenario packs  
  (Amount / Velocity / New Payee / Daily Limit / soft tenancy / MVP seed)  
  + quiet NORMAL continuous traffic (`ACC-QUIET-*` + Flyway V6 history)  
  + paced FRAUD multi-txn traffic + `failedPercent` for `status: FAILED`;  
  plans `.cursor/plans/013-alert-traffic-simulator.md`,  
  `.cursor/plans/014-simulator-quiet-traffic-failed.md`
- [ ] Final unit-test sweep
- [x] Presentation talk track (`docs/PRESENTATION_SCRIPT.md`)


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
