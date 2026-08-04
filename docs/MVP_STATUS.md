# MVP Status — Current Repo State

Last updated: 04 August 2026

This document is the quick source of truth for what is actually present in
the repository now, and how we are proceeding to complete the MVP.

## Architecture snapshot

- Backend is a single Spring Boot modular monolith:
  `api`, `transaction`, `rule`, `alert`, `common`.
- Database is MySQL with Flyway-managed schema migrations.
- Frontend is a React + Vite operator dashboard shell.
- MVP mode is synchronous evaluate-on-record (no queue yet).

## What is completed in repo

1. Project skeleton and foundational conventions are in place.
2. Rule engine skeleton is implemented and tested:
   - `Rule` interface
   - `RuleEngine`
   - `AmountThresholdRule` (hardcoded threshold)
   - Rule evaluation context placeholders
3. Frontend MVP shell exists with:
   - Dashboard / Transactions / Alerts pages
   - bluish theme and app shell
   - typed client scaffolding and sample-data fallback
4. Core architecture and process docs exist:
   - milestones
   - DFD
   - DB design
   - API docs
   - team split and storyline

## What is still pending for MVP

1. Alert backend completion:
   - alert entity/repository
   - `alert_transactions` linking
   - lifecycle transition endpoints
   - `GET /alerts` and `GET /alerts/{id}`
2. Demo hardening:
   - seed/simulate script
   - Swagger/OpenAPI generation
   - complete over-threshold end-to-end run in UI

## How we are proceeding to MVP (execution order)

1. Implement alert persistence and lifecycle APIs in backend.
2. Switch frontend from sample fallback to live API-only responses.
3. Run end-to-end demo path:
   - post normal transactions
   - post over-threshold transaction
   - verify alert appears
   - acknowledge/investigate/close in UI
4. Finalize docs + Swagger and freeze MVP branch.

## Immediate next sprint focus

- Backend alert work first (B): alert create/read/lifecycle APIs.
- Frontend integration second (C): bind existing pages to live endpoints only.
- Team dry-run third (ALL): full presentation script with repeatable seed data.

## MVP definition of done

MVP is done when all of the following are true:

1. Every persisted transaction is synchronously evaluated.
2. Over-threshold transactions create `OPEN` alerts with linked transaction(s).
3. Operator can move alert through valid lifecycle transitions.
4. UI shows transactions, alerts, and lifecycle actions against live backend.
5. Demo scenario runs without manual DB intervention.
