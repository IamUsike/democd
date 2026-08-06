# Alert / traffic simulator refactor

Working plan for Phase 2 TMD-103: turn the standalone Go+React
`transaction-simulator` into a rule-aligned live-demo tool.

Full analysis lives in the Cursor plan
`simulator_verify_refactor_7b934f34` — this file is the repo copy per
`.cursor/plans/README.md`.

## Contract (locked)

`POST /api/simulator/start` body (backward compatible):

```json
{
  "kind": "SCENARIO" | "TRAFFIC",
  "tps": 50,
  "duration": 30,
  "mode": "NORMAL" | "FRAUD",
  "scenario": "AMOUNT_THRESHOLD",
  "sourceType": "BANK" | "MERCHANT" | null,
  "fraudMixPercent": 10
}
```

- `kind` omitted → `TRAFFIC`
- `kind=SCENARIO` → ignore TPS pacing; run pack once
- Scenario IDs: `AMOUNT_THRESHOLD`, `VELOCITY`, `NEW_PAYEE`,
  `DAILY_LIMIT`, `SOFT_TENANCY_MIX`, `MVP_SEED`

## Done when

- One API/UI action per Amount / Velocity / New Payee / Daily Limit fires
  the expected OPEN alert
- FRAUD continuous mode emits full multi-txn sequences
- NORMAL traffic stays under default amount threshold
- Tests green; milestones / README updated
