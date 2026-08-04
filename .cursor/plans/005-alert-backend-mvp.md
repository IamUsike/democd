# Plan: Alert backend MVP (B-3 / B-4 / B-5)

## Goal

Persist alerts when rules fire, expose GET list/detail and PATCH lifecycle
status with validated transitions, and link alerts to transactions via
`alert_transactions`.

## Locked decisions

| Decision | Choice |
|----------|--------|
| Schema | Flyway `V2__create_alerts.sql` — `alerts` + `alert_transactions` |
| Status transitions | OPEN→ACKNOWLEDGED→INVESTIGATING→CLOSED; ACKNOWLEDGED/INVESTIGATING→DISMISSED |
| Create path | `TransactionService.save` → `RuleEngine.evaluate` → `AlertService.createFromMatches` |
| Response shape | `{ success, message, data }` envelope (matches frontend clients) |
| Filters | `GET /alerts?sourceType=&sourceId=&status=` |
| Detail | Include primary `transactionId` + `transactionIds` list |

## Steps

1. Flyway V2 migration
2. Entities + repositories
3. AlertService create + wire from TransactionService
4. Lifecycle updateStatus with invalid-transition tests
5. AlertController GET/PATCH + controller tests
6. Update Project_milestones.md + docs/MVP_STATUS.md

## Result

Shipped:

- Flyway `V2__create_alerts.sql` (`alerts`, `alert_transactions`)
- `Alert` / `AlertTransaction` entities + repositories
- `AlertService` create-from-matches, GET list/detail, validated lifecycle
- `AlertController` with `{ success, message, data }` envelope
- `TransactionService` creates alerts after rule evaluation
- Unit tests for service + controller
- Milestones / MVP_STATUS updated

Not in this slice: seed script, frontend sample-fallback removal, full E2E demo.
