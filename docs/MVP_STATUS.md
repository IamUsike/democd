# MVP Status — Current Repo State

Last updated: 04 August 2026

**Verdict: Phase 1 MVP is ready for live demo.**

Presentation talk track + deep dive:
[`MVP_PRESENTATION.md`](./MVP_PRESENTATION.md).

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
2. Rule engine + sync evaluate-on-record (`AmountThresholdRule`, threshold
   **10000**, strict `>`).
3. Alert create/list/detail/lifecycle APIs with transition validation.
4. Dashboard KPI endpoint `GET /api/v1/dashboard`.
5. Frontend pages wired to live APIs (errors shown if API unavailable).
6. Seed script: `scripts/seed-demo.sh`.
7. springdoc OpenAPI / Swagger UI.
8. CORS for local UI → API.
9. Presentation doc: [`MVP_PRESENTATION.md`](./MVP_PRESENTATION.md).
10. Docker Option A: frontend nginx production image on `:8082` via compose
    (`VITE_API_BASE_URL` build arg for VM).

## Demo runbook

### Local (dev servers)

1. Start MySQL + API (`docker compose up -d mysql` or `./mvnw spring-boot:run`).
2. Start UI: `cd frontend && npm run dev` → `http://localhost:5173`.
3. Seed data: `./scripts/seed-demo.sh`.
4. In UI: Transactions → Alerts → Acknowledge → Investigate → Close.

### Jenkins / Linux VM (Docker Option A)

1. On the agent, set `VITE_API_BASE_URL=http://<VM_PUBLIC_IP>:8081` (job env or `.env`).
2. Pipeline already runs `docker-compose build` / `up -d` (MySQL + API + frontend nginx).
3. Open UI: `http://<VM_IP>:8082/` · API: `http://<VM_IP>:8081`.
4. Seed: `API_BASE=http://<VM_IP>:8081 ./scripts/seed-demo.sh`.
5. Same alert lifecycle walkthrough in the UI.

## What remains

- Live dry-run in front of the team/instructors (ceremony only — mark ALL-1 /
  E2E checkbox in milestones after that room demo).
- Phase 2+ items (extra rules, queue, hardening) — deliberately not started.

## MVP definition of done

1. Every persisted transaction is synchronously evaluated. ✅
2. Over-threshold transactions create `OPEN` alerts with linked transaction(s). ✅
3. Operator can move alert through valid lifecycle transitions. ✅
4. UI shows transactions, alerts, and lifecycle actions against live backend. ✅
5. Demo scenario runs without manual DB intervention. ✅
