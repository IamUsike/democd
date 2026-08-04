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

UI: http://localhost:5173 · API: http://localhost:8081  
Theme direction: bluish operator dashboard (Phase 1).

## Docs

- [`Project_milestones.md`](Project_milestones.md) — build order and status
- [`docs/STORYLINE_AND_KANBAN.md`](docs/STORYLINE_AND_KANBAN.md) — demo story + 3-person kanban
- [`docs/TEAM_WORK_SPLIT.md`](docs/TEAM_WORK_SPLIT.md) — how three people split ownership
- [`docs/DFD-MVP.md`](docs/DFD-MVP.md) — data flows (every txn → rule engine in MVP)
- [`docs/DATABASE_DESIGN.md`](docs/DATABASE_DESIGN.md) — schema (multi-source)
- [`AGENTS.md`](AGENTS.md) — conventions for contributors / agents
