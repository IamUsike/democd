# MVP Status — Current Repo State

Last updated: 04 August 2026

This document is the quick source of truth for what is actually present in
the repository now, and how we are proceeding to complete the MVP.

## Architecture snapshot

- Backend is a single Spring Boot modular monolith:
  `api`, `transaction`, `rule`, `alert`, `common`.
- Database is MySQL with Flyway-managed schema migrations (`V1` transactions,
  `V2` alerts + `alert_transactions`).
- Frontend is a React + Vite operator dashboard shell.
- MVP mode is synchronous evaluate-on-record (no queue yet).

## What is completed in repo

1. Project skeleton and foundational conventions are in place.
2. Transaction ingest/query APIs with soft-tenancy source fields and filters.
3. Rule engine skeleton is implemented and tested:
   - `Rule` / `RuleEngine` / `AmountThresholdRule`
4. Sync path: save transaction → evaluate rules → create OPEN alerts linked
   via `alert_transactions`.
5. Alert APIs:
   - `GET /api/v1/alerts` (optional `sourceType`, `sourceId`, `status`)
   - `GET /api/v1/alerts/{id}` (includes linked transaction id(s))
   - `PATCH /api/v1/alerts/{id}/status` with validated lifecycle transitions
6. Frontend MVP shell exists with Dashboard / Transactions / Alerts pages
   and typed clients (still falls back to sample data if APIs fail).

## What is still pending for MVP

1. Demo hardening:
   - seed/simulate script for BANK + MERCHANT traffic
   - Swagger/OpenAPI polish if not already usable via springdoc
   - remove frontend sample-data fallback once live APIs are verified
2. End-to-end demo dry-run:
   - over-threshold POST → alert appears in UI → acknowledge/close

## How we are proceeding to MVP (execution order)

1. Point frontend at live alert/transaction APIs and clear sample fallback.
2. Add seed/simulate script for repeatable demo data.
3. Run end-to-end demo path as a team.
4. Freeze MVP branch.

## Immediate next sprint focus

- Frontend live integration (C): drop sample fallback when backend is up.
- Seed + Swagger polish (A).
- Team dry-run (ALL): over-threshold → alert → lifecycle in UI.

## MVP definition of done

MVP is done when all of the following are true:

1. Every persisted transaction is synchronously evaluated.
2. Over-threshold transactions create `OPEN` alerts with linked transaction(s).
3. Operator can move alert through valid lifecycle transitions.
4. UI shows transactions, alerts, and lifecycle actions against live backend.
5. Demo scenario runs without manual DB intervention.
