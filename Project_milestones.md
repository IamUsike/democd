# Project Milestones — Transaction Monitoring & Alerts

Update this file as milestones complete. Cursor agents read this before
starting work — keep "Current status" honest and current, not aspirational.

## Current status
**Phase:** Not started / MVP in progress *(edit me)*
**Last updated:** *(edit me)*
**Currently working on:** *(edit me — e.g. "Transaction service + amount
threshold rule")*

---

## Phase 1 — MVP

Build in this order. Don't start an item until the one above it works and
is tested.

- [ ] Project skeleton — Spring Boot app, package structure per
      `backend-java.mdc`, migration tool configured, git repo pushed
- [ ] Transaction entity + repository (fields: id, account_id, payee_id,
      amount, currency, type, timestamp, description, status)
- [ ] `POST /transactions`, `GET /transactions` (list + filter/search)
- [ ] Rule engine skeleton — `Rule` interface, `RuleEngine`,
      `AmountThresholdRule` (single hardcoded threshold)
- [ ] Wire transaction recording → synchronous rule evaluation
- [ ] Alert entity + repository (status, severity, rule reference,
      created/acknowledged/closed timestamps, resolution notes)
- [ ] Alert lifecycle endpoints — acknowledge, investigate, close,
      dismiss — with transition validation
- [ ] `GET /alerts`, `GET /alerts/{id}` (with triggering transactions)
- [ ] React UI: transaction list (filter/search)
- [ ] React UI: alert list + alert detail + lifecycle action buttons
- [ ] Swagger/OpenAPI docs generated
- [ ] Test data generator (simple script or endpoint hitting the API)
- [ ] End-to-end MVP demo: post a transaction over threshold, see the
      alert appear, acknowledge and close it, from the UI

## Phase 2 — additional rule types

- [ ] `VelocityRule` (N transactions within T minutes)
- [ ] `NewPayeeRule` (first transaction to an unseen payee)
- [ ] `DailyLimitRule` (cumulative daily amount)
- [ ] Severity levels (HIGH/MEDIUM/LOW) surfaced in UI with color coding
- [ ] Rules management UI (view; edit is stretch)

## Phase 3 — scale-out

- [ ] Message queue between transaction recording and rule evaluation
- [ ] Rule engine extracted as its own deployable, calling back into the
      monolith's internal alert-creation endpoint
- [ ] Horizontal scaling of rule engine instances demonstrated
- [ ] Cache for velocity/daily-limit count lookups
- [ ] Read replica for reporting/dashboard queries

## Phase 4 — security & hardening

- [ ] TLS confirmed end-to-end (client↔API at minimum)
- [ ] Database encryption at rest enabled
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

## Architecture reference
See `.cursor/rules/general.mdc` for the modular monolith decision and
build-order rationale. HLD/LLD diagrams live wherever the team has put
them (Miro/Figma/Drive link — *add link here*).
