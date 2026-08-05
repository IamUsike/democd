# Frontend/Backend Handoff — High Ingestion Visibility (Frontend-Only)

## Scope of this change

This update is **frontend-only**.

- No backend code changed
- No database migrations changed
- No API contract changed

The goal is to help operators avoid missing transactions during high-ingestion bursts.

## What frontend now does

On `Transactions` page:

- Auto-refresh toggle (3s polling)
- Pause/Resume feed control
- Manual `Refresh now` button
- Quick client-side search across visible rows
- Pin transaction by `transactionId` (keeps row at top)
- Visual highlight for newly arrived rows between refreshes
- Last updated timestamp shown near result count

## API expectations (unchanged)

Frontend still uses existing endpoint and params:

- `GET /api/v1/transactions`
- Optional filters: `sourceType`, `sourceId`, `accountId`
- Expected response: existing `ApiResponse` envelope with transaction list in `data`

## Why this helps under load

Even if many rows arrive quickly, operators can:

- Pause feed to inspect a stable table
- Pin a critical transaction so it stays visible
- Quickly search for account/source/payee text
- Notice newly ingested rows immediately

## Backend teammate context

No backend action is required for this UI update to work.

Optional future backend improvements (not part of this change):

- Server-side pagination and sorting for very large datasets
- Server-side full-text search
- Cursor-based pagination for near-real-time feeds

## Files changed in this frontend-only update

- `frontend/src/hooks/useTransactions.ts`
- `frontend/src/pages/TransactionsPage.tsx`
- `frontend/src/components/TransactionsPanel.tsx`
- `frontend/src/App.css`
- `frontend/README.md`
- `README.md`
- `docs/FRONTEND_BACKEND_HANDOFF.md`

