# Data Flow Diagram — MVP Transaction Monitoring

## Short answer

**Yes.** In MVP, **every** successfully recorded transaction is passed to the
rule engine **synchronously** (same request, no queue). Sources are banks or
merchants (usually **simulated** via the public ingest API). The engine runs
all active rules (today: Amount Threshold only). If a rule fires, an alert is
created and linked before the API responds.

Phase 3 changes *when* evaluation happens (via a queue), not *whether* each
transaction is evaluated. Soft tenancy: one DB; rows carry `source_type`,
`source_id`, and `source_name`.

---

## Level 0 — Context

```mermaid
flowchart LR
  Operator[Operator]
  BankSim[Bank simulator]
  MerchSim[Merchant simulator]
  System[Transaction Monitoring System]
  DB[(MySQL soft tenancy)]

  Operator -->|"view txns / alerts / KPIs, lifecycle"| System
  BankSim -->|"POST ingest source_type BANK"| System
  MerchSim -->|"POST ingest source_type MERCHANT"| System
  System <-->|"read / write"| DB
```

Bank and merchant “feeds” in MVP are public API clients that POST the same
ingest contract (`POST /api/v1/transactions`) with different `source_*`
fields — not separate integration stacks.

---

## Level 1 — Record transaction & evaluate rules (happy path)

```mermaid
flowchart TB
  Sim[Bank or Merchant simulator]
  API[API Layer]
  TxnSvc[Transaction Service]
  Engine[Rule Engine]
  Rules[AmountThresholdRule]
  AlertSvc[Alert Service]
  DB[(MySQL)]

  Sim -->|"1. POST txn + source_type id name"| API
  API -->|"2. record request"| TxnSvc
  TxnSvc -->|"3. INSERT transaction"| DB
  TxnSvc -->|"4. evaluate saved txn"| Engine
  Engine -->|"5. evaluate txn + context"| Rules
  Rules -->|"6. alert candidates or empty"| Engine
  Engine -->|"7. if any alerts"| AlertSvc
  AlertSvc -->|"8. INSERT alert + alert_transactions"| DB
  TxnSvc -->|"9. response txn + optional alert"| API
  API -->|"10. 201 Created"| Sim
```

### Data stores (logical)

| Store | Written on this path | Read on this path |
|-------|----------------------|-------------------|
| `transactions` | Every POST | Later GETs; Phase 2 rules also read for velocity/payee/limits |
| `alerts` | Only if a rule fires | Operator GET / lifecycle |
| `alert_transactions` | Only if a rule fires | Alert detail |

### Important behaviors

| Case | Rule engine called? | Alert created? |
|------|---------------------|----------------|
| Amount above threshold | Yes | Yes (`OPEN`) |
| Amount at/below threshold | Yes | No |
| Validation failure (bad payload) | No — never persisted | No |
| Persist fails | No evaluation | No |

So: **every persisted transaction** is evaluated; not every HTTP request.

---

## Level 1 — Operator alert lifecycle (separate flow)

```mermaid
flowchart LR
  Operator[Operator / UI]
  API[API Layer]
  AlertSvc[Alert Service]
  DB[(MySQL)]

  Operator -->|"GET /alerts filter by source"| API
  Operator -->|"acknowledge / investigate / close / dismiss"| API
  API --> AlertSvc
  AlertSvc <-->|"read / update status"| DB
```

---

## Level 1 — Query transactions & KPIs (no rule engine)

```mermaid
flowchart LR
  Client[Client / UI]
  API[API Layer]
  TxnSvc[Transaction Service]
  DB[(MySQL)]

  Client -->|"GET /transactions filters by source"| API
  Client -->|"GET dashboard KPI aggregates"| API
  API --> TxnSvc
  TxnSvc -->|"SELECT / COUNT GROUP BY"| DB
  TxnSvc --> API
  API --> Client
```

List/search and KPI aggregations do **not** re-run the rule engine.

---

## Phase 3 contrast (not MVP)

```mermaid
flowchart LR
  TxnSvc[Transaction Service]
  Q[Message Queue]
  Engine[Rule Engine Workers]
  AlertSvc[Alert Service]
  DB[(MySQL)]

  TxnSvc -->|"persist txn, publish event"| DB
  TxnSvc --> Q
  Q -->|"txn id / payload"| Engine
  Engine --> AlertSvc
  AlertSvc --> DB
```

MVP skips the queue so MTTD stays as low as a single request allows:
POST → maybe alert in the same response (or immediately on `GET /alerts`).

---

## Module mapping

| DFD process | Package |
|-------------|---------|
| API Layer | `com.example.txnmonitor.api` |
| Transaction Service | `com.example.txnmonitor.transaction` |
| Rule Engine + rules | `com.example.txnmonitor.rule` |
| Alert Service | `com.example.txnmonitor.alert` |
| MySQL | Flyway schema — see [`DATABASE_DESIGN.md`](./DATABASE_DESIGN.md) |

---

*Aligned with soft multi-source tenancy (1A), lean MVP (2A), `AGENTS.md`,
`rule-engine.mdc`, and Phase 1 of `Project_milestones.md`.*
