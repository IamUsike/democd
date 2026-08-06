# 008 — List pagination + UI smoothness at volume

## Goal
Make alerts/transactions lists usable at load-test volumes (lakhs of rows)
without shipping the full table to the browser on every poll.

## Breaking API change
`GET /api/v1/alerts` and `GET /api/v1/transactions` `data` becomes a page
envelope (not a bare array):

```json
{
  "items": [],
  "totalCount": 0,
  "page": 0,
  "size": 50,
  "hasNext": false
}
```

Frontend updated in the same change.

## Backend
1. `PageResponse<T>` DTO in `api`
2. Repositories: add `JpaSpecificationExecutor`
3. `GET /alerts` params: `page`, `size`, `sourceType`, `sourceId`, `status`,
   `severity`, `accountId`, `q`, `createdFrom`, `createdTo`, `sort`
   - Default when `status` omitted: active only (`OPEN|ACKNOWLEDGED|INVESTIGATING`)
   - `status=ALL` → no status filter
   - Default sort: `createdAt,desc`
   - List responses omit linked `transactionIds` (summary); detail still loads them
4. `GET /transactions` params: `page`, `size`, `sourceType`, `sourceId`,
   `accountId`, `q`, `from`, `to`, `afterId`, `sort`
   - Default sort: `timestamp,desc` (or `transactionId,asc` when `afterId` set)
   - `afterId` → only rows with `transactionId > afterId` (delta poll)
5. TDD: controller + service tests for page shape, defaults, afterId, filters

## Frontend
1. Shared `PageResponse` type; update clients/hooks
2. Alerts: server filters (status/severity/source), pagination controls,
   lazy detail via existing `GET /alerts/{id}`, skeleton rows
3. Transactions: paginated browse + auto-refresh via `afterId` (no full
   re-fetch); debounced `q`; virtualized table body
4. Add `@tanstack/react-virtual`

## Docs
- `docs/API_ENDPOINT.md` sections 3.2 / 4.1
- `docs/FRONTEND_BACKEND_HANDOFF.md`
- `frontend/README.md`
- `Project_milestones.md` (note pagination shipped)

## Out of scope
Cursor tokens beyond `afterId`, infinite scroll UX polish, read replica,
changing path-based txn endpoints (`/account/{id}` etc.).
