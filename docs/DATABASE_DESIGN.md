# Transaction Monitoring System

# Database Design Document

**Version:** 1.2

**Project:** Transaction Monitoring & Alert Dashboard

**Prepared By:** <Your Name>

**Date:** 04 August 2026

---

# 1. Introduction

## Purpose

This document describes the database design for the Transaction Monitoring
System: schema, tables, relationships, keys, and indexes.

The system uses a relational database (MySQL for MVP). Schema changes go
through Flyway migrations — not `ddl-auto: update`.

The ER diagram lives at
[`docs/transaction monitoring er diagram.mmd`](./transaction%20monitoring%20er%20diagram.mmd).

---

# 2. Database Overview

## Design decisions (locked)

1. **Soft multi-source tenancy.** Transactions arrive from **banks** and
   **merchants** (simulated via the public ingest API in MVP). One
   database; every row carries `source_type` (`BANK` | `MERCHANT`),
   `source_id` (stable code), and `source_name` (display name). No
   per-bank schemas and **no `sources` master table** in MVP — names are
   denormalized on the transaction (same pattern as payee).
2. **No `accounts` or `payees` master tables.** Opaque `account_id` /
   `payee_id` plus display fields (`payee_name`) live on the transaction
   row. Phase 2 rules query those columns directly.
3. **No `rules` / `rule_parameters` tables in MVP.** Amount Threshold is
   hardcoded. Alerts store `rule_type` as a string. User-configurable
   rules are Phase 2.
4. **Alerts link via `alert_transactions`.** Amount Threshold attaches
   one txn; Velocity (Phase 2) can attach many.
5. **KPIs are aggregations, not columns.** Dashboard metrics (counts by
   source, open alerts, MTTD proxy) are computed in queries / API — not
   stored on each transaction.

## MVP tables

1. `transactions`
2. `alerts`
3. `alert_transactions`

Whenever a transaction is recorded, the rule engine evaluates it
**synchronously** (MVP MTTD goal: detect in the same request). If a rule
triggers, an alert is created and linked.

---

# 3. Database Tables

## 3.1 Transactions

### Purpose

Stores every financial transaction ingested from a bank or merchant feed
(including simulators).

### Table Name

```
transactions
```

### Columns

| Column | Data Type | Constraints | Description |
|----------|-----------|-------------|-------------|
| transaction_id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique transaction identifier |
| source_type | VARCHAR(20) | NOT NULL | `BANK` or `MERCHANT` |
| source_id | VARCHAR(64) | NOT NULL | Opaque source code (e.g. `HSBC-UK`, `ACME-POS`) |
| source_name | VARCHAR(128) | NOT NULL | Human-readable source name for UI / sims |
| account_id | VARCHAR(64) | NOT NULL | Monitored party (opaque ID — not an FK) |
| payee_id | VARCHAR(64) | NOT NULL | Counterparty / payee (opaque ID — not an FK) |
| payee_name | VARCHAR(128) | NULL | Payee display name |
| amount | DECIMAL(15,2) | NOT NULL | Transaction amount |
| currency | VARCHAR(3) | NOT NULL | Currency code (e.g. USD, INR) |
| type | VARCHAR(30) | NOT NULL | e.g. DEBIT, CREDIT, TRANSFER |
| timestamp | DATETIME | NOT NULL | When the transaction occurred |
| location | VARCHAR(255) | NULL | Free-text location (city / country / label) |
| latitude | DECIMAL(10,7) | NULL | Optional geo |
| longitude | DECIMAL(10,7) | NULL | Optional geo |
| description | VARCHAR(255) | NULL | Optional description |
| status | VARCHAR(20) | NOT NULL | e.g. COMPLETED, FAILED |

### Constraints

- `amount` must be greater than zero.
- `source_type` must be `BANK` or `MERCHANT`.
- `source_id`, `source_name`, `account_id`, and `payee_id` are mandatory
  (non-blank).

### Indexes (scalability)

| Index | Columns | Why |
|-------|---------|-----|
| PK | `transaction_id` | Lookup by id |
| `idx_txn_account_timestamp` | `(account_id, timestamp)` | Velocity / Daily Limit / account filters |
| `idx_txn_account_payee` | `(account_id, payee_id)` | New Payee checks |
| `idx_txn_source_timestamp` | `(source_type, source_id, timestamp)` | Per-source lists and KPIs |

---

## 3.2 Alerts

### Purpose

Stores alerts generated when a rule fires. Lifecycle status is enforced
in the alert service.

### Table Name

```
alerts
```

### Columns

| Column | Data Type | Constraints | Description |
|----------|-----------|-------------|-------------|
| alert_id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique alert identifier |
| rule_type | VARCHAR(64) | NOT NULL | e.g. `AMOUNT_THRESHOLD` (no FK to rules in MVP) |
| account_id | VARCHAR(64) | NOT NULL | Denormalized from triggering txn(s) |
| source_type | VARCHAR(20) | NOT NULL | Denormalized from triggering txn |
| source_id | VARCHAR(64) | NOT NULL | Denormalized from triggering txn |
| source_name | VARCHAR(128) | NOT NULL | Denormalized for list UI |
| status | VARCHAR(30) | NOT NULL | OPEN / ACKNOWLEDGED / INVESTIGATING / CLOSED / DISMISSED |
| severity | VARCHAR(20) | NOT NULL | LOW / MEDIUM / HIGH |
| created_at | DATETIME | NOT NULL | Alert creation time |
| acknowledged_at | DATETIME | NULL | When acknowledged |
| investigating_at | DATETIME | NULL | When investigation started |
| dismissed_at | DATETIME | NULL | When dismissed |
| closed_at | DATETIME | NULL | When closed |
| resolution_notes | VARCHAR(1000) | NULL | Notes on close/dismiss |

### Valid status transitions (enforced in service)

```
OPEN → ACKNOWLEDGED → INVESTIGATING → CLOSED
ACKNOWLEDGED → DISMISSED
INVESTIGATING → DISMISSED
```

### Indexes

| Index | Columns | Why |
|-------|---------|-----|
| PK | `alert_id` | Lookup by id |
| `idx_alert_status` | `(status)` | Active alerts list |
| `idx_alert_account` | `(account_id)` | Filter by account |
| `idx_alert_source` | `(source_type, source_id)` | Filter by bank/merchant |
| `idx_alert_created` | `(created_at)` | History / ordering |

---

## 3.3 Alert Transactions

### Purpose

Many-to-many link between alerts and contributing transaction(s).

### Table Name

```
alert_transactions
```

### Columns

| Column | Data Type | Constraints | Description |
|----------|-----------|-------------|-------------|
| alert_id | BIGINT | PK, FK → alerts(alert_id) | Alert |
| transaction_id | BIGINT | PK, FK → transactions(transaction_id) | Linked transaction |

### Primary Key

Composite: `(alert_id, transaction_id)`

---

# 4. KPIs (aggregations — not stored columns)

Compute in the dashboard / reporting API:

| KPI | Sketch |
|-----|--------|
| Transactions by source | `COUNT(*) … GROUP BY source_type, source_id` |
| Open alerts | `COUNT(*) FROM alerts WHERE status = 'OPEN'` |
| Alerts by severity | `GROUP BY severity` |
| MTTD proxy | Avg of (`alerts.created_at` − linked `transactions.timestamp`) |

Phase 1 can expose simple counts; charts are optional polish.

---

# 5. Entity Relationships

| Parent | Child | Relationship |
|--------|-------|--------------|
| alerts | alert_transactions | One-to-Many |
| transactions | alert_transactions | One-to-Many |

`source_*`, `account_id`, and `payee_id` are **not** FKs to master tables.

```
transactions (1) ----< alert_transactions >---- (1) alerts
```

---

# 6. Explicitly out of MVP schema

| Table / idea | When |
|--------------|------|
| `sources` master | Only if shared source metadata becomes painful to denormalize |
| `accounts`, `payees` | Only if managing master data |
| `rules`, `rule_parameters` | Phase 2 — user-configurable rules |
| `alert_status_history` | Phase 4 audit trail |
| users / auth tables | Out of scope — single operator, no auth |
| Application-level field encryption columns | Phase 4 — prefer DB encryption at rest + API/UI masking |

---

# 7. Assumptions

- Ingest is via public REST API; bank/merchant feeds are **simulated** in MVP.
- Soft tenancy only — one schema for all sources.
- Rule evaluation is synchronous in MVP (no queue yet).
- Amount Threshold is hardcoded in the rule constructor.
- Authentication is outside MVP scope.
- If Flyway `V1` is rewritten after a local apply, recreate the
  `txnmonitor` database or Docker volume so checksums stay valid.

---

# 8. Document Information

| Item | Value |
|------|-------|
| Document Name | Database Design Document |
| Project | Transaction Monitoring System |
| Version | 1.2 |
| Database | MySQL (MVP) |
| Status | Soft multi-source + enriched txn fields |
| Last Updated | 04 August 2026 |
