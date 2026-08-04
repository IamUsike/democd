# Transaction Monitoring frontend

React + TypeScript operator dashboard for Phase 1 MVP.

## What is implemented

- AGIL-ish PWA-style shell with left navigation
- Separate routes: Dashboard, Transactions, Alerts
- Dark Obsidian & Slate theme with multi-color accents (Indigo, Cyan, Emerald, Amber, Rose/Crimson)
- Zero horizontal scroll layout with responsive data tables
- KPI strip (`GET /api/v1/dashboard`)
- Transactions list with source/account filters (`GET /api/v1/transactions`)
- Active alerts list (`GET /api/v1/alerts`)
- Alert detail view (`GET /api/v1/alerts/{id}`)
- Lifecycle action buttons (`PATCH /api/v1/alerts/{id}/status`)
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

For MVP demo flow, if backend endpoints are not available yet, the UI falls back to sample data and shows a warning banner.
