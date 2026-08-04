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

## Frontend (local dev)

```bash
cd frontend
npm install
npm run dev
```

UI: `http://localhost:5173` · API default: `http://localhost:8081`

Implements MVP frontend scope:
- AGIL-ish left-nav app shell (PWA-style layout)
- Separate pages for Dashboard, Transactions, and Alerts
- Dark Obsidian & Slate theme with multi-color accents & zero horizontal scroll layout
- Live API clients only (no sample-data fallback)
- KPI strip (`GET /api/v1/dashboard`)
- Transaction list with source filters (`GET /api/v1/transactions`)
- Active alerts list + detail (`GET /api/v1/alerts`, `GET /api/v1/alerts/{id}`)
- Lifecycle actions (`PATCH /api/v1/alerts/{id}/status`)

### Seed demo data

```bash
./scripts/seed-demo.sh
# or against a remote API:
API_BASE=http://your-vm:8081 ./scripts/seed-demo.sh
```

Build check:

```bash
cd frontend
npm run build
```

## Docker / Jenkins VM deploy (Option A)

Jenkins runs `docker-compose build` + `up -d`. Compose starts **MySQL**,
**Spring Boot (:8081)**, and a **frontend nginx** container serving the
production Vite build on **:8082** (mapped to nginx internal `:80`).

| Surface | URL |
|---------|-----|
| UI | `http://<VM_IP>:8082/` |
| API | `http://<VM_IP>:8081` |
| Swagger | `http://<VM_IP>:8081/swagger-ui.html` |

`VITE_API_BASE_URL` is baked into the frontend image at **build** time. On the
VM it must be the address browsers use (not `localhost` if you open the UI
from another machine):

```bash
# copy and edit
cp .env.example .env
# set e.g. VITE_API_BASE_URL=http://10.0.0.12:8081

docker compose build --no-cache
docker compose up -d
```

Or one-shot without a `.env` file:

```bash
VITE_API_BASE_URL=http://<VM_IP>:8081 docker compose build --no-cache frontend
docker compose up -d
```

Seed against the VM API:

```bash
API_BASE=http://<VM_IP>:8081 ./scripts/seed-demo.sh
```

Jenkins tip: set `VITE_API_BASE_URL` as a job/environment variable on the agent
so `docker-compose build` picks it up via `${VITE_API_BASE_URL}` in
[`docker-compose.yml`](docker-compose.yml). The [`Jenkinsfile`](Jenkinsfile)
does not need stage changes for Option A.

## Docs

- [`docs/MVP_PRESENTATION.md`](docs/MVP_PRESENTATION.md) — presentation talk track + in-depth system explanation
- [`Project_milestones.md`](Project_milestones.md) — build order and status
- [`docs/MVP_STATUS.md`](docs/MVP_STATUS.md) — current repo progress (MVP demo-ready)
- [`docs/KANBAN.md`](docs/KANBAN.md) — live sprint board
- [`docs/STANDUP_LOG.md`](docs/STANDUP_LOG.md) — daily stand-up notes
- [`docs/STORYLINE_AND_KANBAN.md`](docs/STORYLINE_AND_KANBAN.md) — product storyline + story IDs
- [`docs/TEAM_WORK_SPLIT.md`](docs/TEAM_WORK_SPLIT.md) — ownership split
- [`docs/DFD-MVP.md`](docs/DFD-MVP.md) — data flows (every txn → rule engine in MVP)
- [`docs/DATABASE_DESIGN.md`](docs/DATABASE_DESIGN.md) — schema (multi-source)
- [`AGENTS.md`](AGENTS.md) — conventions for contributors / agents
