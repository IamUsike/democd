# Frontend/Backend Handoff — List pagination & smooth UI at volume

## Scope

Cross-stack change (backend + frontend) so alerts/transactions lists stay
usable under load-test volumes.

Plan: [`.cursor/plans/008-list-pagination-ui-smoothness.md`](../.cursor/plans/008-list-pagination-ui-smoothness.md)

## API contract (breaking)

`GET /api/v1/alerts` and `GET /api/v1/transactions` now return a page
envelope in `data`:

```json
{
  "items": [],
  "totalCount": 0,
  "page": 0,
  "size": 50,
  "hasNext": false
}
```

See [`API_ENDPOINT.md`](API_ENDPOINT.md) sections 3.2 and 4.1.

### Alerts defaults

- Omit `status` → active only (`OPEN`, `ACKNOWLEDGED`, `INVESTIGATING`)
- `status=ALL` → full history
- List responses omit linked transaction IDs; detail via `GET /alerts/{id}`

### Transactions delta poll

- Initial / paged browse: `page`, `size`, filters, `sort`
- Live feed: poll with `afterId={maxSeenId}` (no full-page re-fetch)

## Frontend behavior

- Paginated alerts + transactions with Previous/Next
- Server-side filters, sort, debounced `q`
- Transaction auto-refresh uses `afterId` only
- Virtualized lists (`@tanstack/react-virtual`)
- Skeleton rows while loading; alert detail lazy-loaded

## Files (high level)

**Backend:** `PageResponse`, `PageRequestFactory`, alert/transaction
specifications + service/controller updates, tests.

**Frontend:** typed clients/hooks, `AlertsPage` / `TransactionsPage`
panels, CSS for virtual lists + skeletons.
