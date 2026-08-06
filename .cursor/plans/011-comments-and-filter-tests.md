# 011 — Comments + filter tests (alert_vscroll)

## Scope
Comment the alerts/transactions list filter + virtual-scroll path only
(not the entire monorepo). Add backend tests for status/q/severity filter
gaps uncovered by existing AlertService / AlertController tests.

## Frontend comments
Brief section comments on AlertsPage, AlertsPanel, TransactionsPanel,
useAlerts, useTransactions, alertsClient — why filters were slimmed and
how virtualization / debounce work.

## Tests
- `resolveStatuses`: blank → active set; ALL → null; single status
- `getAlerts` passes severity + q into specification path (mock verify)
- Controller: `status=ALL` forwarded
