# Transaction Monitoring & Alerts Dashboard

Spring Boot API + React operator dashboard. Ingests transactions from
**banks** and **merchants** (simulated in MVP via the public API), evaluates
them against rules, and manages alerts through a lifecycle.

Soft tenancy: one database; each transaction carries `sourceType`,
`sourceId`, and `sourceName`.

## Backend

```bash
# Start MySQL (creates txnmonitor database)
docker compose up -d mysql

# Run the API (dev profile, port 8081)
./mvnw spring-boot:run
```

Flyway applies migrations from `src/main/resources/db/migration/` on startup.
JPA `ddl-auto` is `validate` — schema changes belong in Flyway only.

If `V1` was rewritten after you already ran migrations locally, recreate
the `txnmonitor` database or Docker volume so Flyway checksums stay valid.

Package layout: `com.example.txnmonitor` → `api`, `transaction`, `rule`,
`alert`, `common`.

## Frontend

```bash
cd frontend
npm install
npm run dev
```

UI: `http://localhost:5173` · API default: `http://localhost:8081`

Implemented MVP frontend scope:
- AGIL-ish left-nav app shell (PWA-style layout)
- Separate pages for Dashboard, Transactions, and Alerts
- Bluish Huddle-style theme kept across pages
- KPI strip (`GET /api/v1/dashboard`)
- Transaction list with source filters (`GET /api/v1/transactions`)
- Active alerts list + detail (`GET /api/v1/alerts`, `GET /api/v1/alerts/{id}`)
- Lifecycle actions (`PATCH /api/v1/alerts/{id}/status`)

Build check:

```bash
cd frontend
npm run build
```

## Docs

- [`Project_milestones.md`](Project_milestones.md) — build order and status
- [`docs/KANBAN.md`](docs/KANBAN.md) — live sprint board
- [`docs/STANDUP_LOG.md`](docs/STANDUP_LOG.md) — daily stand-up notes
- [`docs/STORYLINE_AND_KANBAN.md`](docs/STORYLINE_AND_KANBAN.md) — product storyline + story IDs
- [`docs/TEAM_WORK_SPLIT.md`](docs/TEAM_WORK_SPLIT.md) — ownership split
- [`docs/DFD-MVP.md`](docs/DFD-MVP.md) — data flows (every txn → rule engine in MVP)
- [`docs/DATABASE_DESIGN.md`](docs/DATABASE_DESIGN.md) — schema (multi-source)
- [`AGENTS.md`](AGENTS.md) — conventions for contributors / agents
