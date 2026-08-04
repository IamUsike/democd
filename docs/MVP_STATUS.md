# MVP Status — Current Repo State

Last updated: 04 August 2026

This document is the quick source of truth for what is actually present in
the repository now, and how we are proceeding to complete the MVP.

## Architecture snapshot

- Backend is a single Spring Boot modular monolith:
  `api`, `transaction`, `rule`, `alert`, `common`.
- Database is MySQL with Flyway-managed schema migrations (`V1` transactions,
  `V2` alerts + `alert_transactions`).
- Frontend is a React + Vite operator dashboard (live API only; no sample
  fallback).
- MVP mode is synchronous evaluate-on-record (no queue yet).

## What is completed in repo

1. Transaction ingest/query APIs with soft-tenancy fields + envelope responses.
2. Rule engine + sync evaluate-on-record.
3. Alert create/list/detail/lifecycle APIs.
4. Dashboard KPI endpoint `GET /api/v1/dashboard`.
5. Frontend pages wired to live APIs (errors shown if API unavailable).
6. Seed script: `scripts/seed-demo.sh`.
7. springdoc OpenAPI available with the app (Swagger UI).

## Demo runbook

1. Start MySQL + API (`docker compose up -d` or `./mvnw spring-boot:run`).
2. Start UI: `cd frontend && npm run dev`.
3. Seed data: `./scripts/seed-demo.sh` (or `API_BASE=http://host:8081 ./scripts/seed-demo.sh`).
4. In UI: Transactions list fills; Alerts shows the over-threshold OPEN alert;
   walk Acknowledge → Investigate → Close.

## What remains

- Team end-to-end dry-run of the demo script above (mark ALL-1 done after).

## MVP definition of done

1. Every persisted transaction is synchronously evaluated.
2. Over-threshold transactions create `OPEN` alerts with linked transaction(s).
3. Operator can move alert through valid lifecycle transitions.
4. UI shows transactions, alerts, and lifecycle actions against live backend.
5. Demo scenario runs without manual DB intervention.
