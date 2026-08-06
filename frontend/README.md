# Transaction Monitoring frontend

React + TypeScript operator dashboard for Phase 1 MVP.

## What is implemented

- AGIL-ish PWA-style shell with left navigation
- Separate routes: Dashboard, Transactions, Alerts
- Dark Obsidian & Slate theme with multi-color accents (Indigo, Cyan, Emerald, Amber, Rose/Crimson)
- Zero horizontal scroll layout with responsive data tables
- KPI strip (`GET /api/v1/dashboard`)
- **Paginated** transactions list with server filters/sort/search (`GET /api/v1/transactions`)
- High-ingestion controls: auto-refresh via `afterId` delta poll, pause/resume, manual page refresh, pin by ID, new-row highlight
- Virtualized transaction rows (`@tanstack/react-virtual`) + skeleton loading
- **Paginated** alerts list with status/severity/source filters (default: active only)
- Alert detail lazy-loaded (`GET /api/v1/alerts/{id}`) including triggering transactions
- Lifecycle action buttons (`PATCH /api/v1/alerts/{id}/status`)
- Debounced search on both list screens
- Typed API client modules (`api/transactionsClient`, `api/alertsClient`, `api/dashboardClient`)
- Hook-based local state (no global state library)

## Run

```bash
npm install
npm run dev
```

Dev server: `http://localhost:5173`

## Build check

```bash
npm run build
```

## API base URL

Default: `http://localhost:8081`

Override with Vite env variable:

```bash
VITE_API_BASE_URL=http://localhost:8081
```

For Docker / Jenkins (production static build + nginx), see the root README
“Docker / Jenkins VM deploy” section. Build arg / `.env`:

```bash
VITE_API_BASE_URL=http://<VM_IP>:8081
```

UI then: `http://<VM_IP>:8082/` · API: `http://<VM_IP>:8081`

## Docker image

```bash
# from repo root
docker compose build frontend
docker compose up -d frontend
```

`frontend/Dockerfile` multi-stage: `npm run build` → `nginx:alpine` on port 80.
SPA routes fall back via `frontend/nginx.conf`.
