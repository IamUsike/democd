# Transaction Monitoring System

# Database Design Document

**Version:** 1.1

**Project:** Transaction Monitoring & Alert Dashboard

**Prepared By:** <Your Name>

**Date:** 03 August 2026

---

# 1. Introduction

## Purpose

This document describes the database design for the Transaction Monitoring
System: schema, tables, relationships, keys, constraints, and indexes.

The system uses a relational database (MySQL for MVP). Schema changes go
through Flyway/Liquibase migrations — not `ddl-auto: update`.

The ER diagram lives at
[`docs/transaction monitoring er diagram.mmd`](./transaction%20monitoring%20er%20diagram.mmd).

---

# 2. Database Overview

## Design decisions (locked)

1. **No `accounts` or `payees` master tables.** This system records
   transactions and evaluates rules; it does not create or manage
   accounts/payees. `account_id` and `payee_id` are opaque string
   identifiers stored on each transaction row. Phase 2 rules (Velocity,
   New Payee, Daily Limit) query those columns directly — the same
   pattern as the project brief SQL examples.
2. **No `rules` / `rule_parameters` tables in MVP.** The Amount Threshold
   rule is hardcoded in the rule engine. Alerts store a `rule_type`
   string (e.g. `AMOUNT_THRESHOLD`) so the UI can show which rule fired.
   A configurable rules table is a later enhancement.
3. **Alerts link to transactions via a junction table.** Amount Threshold
   attaches one transaction; Velocity (Phase 2) can attach many. Use
   `alert_transactions` from day one so the model does not need a
   breaking change later.

## MVP tables

1. `transactions`
2. `alerts`
3. `alert_transactions`

Whenever a transaction is recorded, the rule engine evaluates it
synchronously. If a rule triggers, an alert is created and linked to the
relevant transaction(s).

---

# 3. Database Tables

## 3.1 Transactions

### Purpose

Stores every financial transaction received by the system.

### Table Name

```
transactions
```

### Columns

| Column | Data Type | Constraints | Description |
|----------|-----------|-------------|-------------|
| transaction_id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique transaction identifier |
| account_id | VARCHAR(64) | NOT NULL | Monitored party (opaque ID — not an FK) |
| payee_id | VARCHAR(64) | NOT NULL | Counterparty / payee (opaque ID — not an FK) |
| amount | DECIMAL(15,2) | NOT NULL | Transaction amount |
| currency | VARCHAR(3) | NOT NULL | Currency code (e.g. USD, INR) |
| type | VARCHAR(30) | NOT NULL | e.g. DEBIT, CREDIT, TRANSFER |
| timestamp | DATETIME | NOT NULL | When the transaction occurred |
| description | VARCHAR(255) | NULL | Optional description |
| status | VARCHAR(20) | NOT NULL | e.g. COMPLETED, FAILED |

### Constraints

- `amount` must be greater than zero.
- `account_id` and `payee_id` are mandatory (non-blank).

### Indexes (scalability)

| Index | Columns | Why |
|-------|---------|-----|
| PK | `transaction_id` | Lookup by id |
| `idx_txn_account_timestamp` | `(account_id, timestamp)` | Velocity / Daily Limit / account filters |
| `idx_txn_account_payee` | `(account_id, payee_id)` | New Payee checks |

These indexes matter more for scale than introducing Account/Payee tables.

---

## 3.2 Alerts

### Purpose

Stores alerts generated when a rule fires. Lifecycle status is enforced
in the alert service (not only by a DB check constraint).

### Table Name

```
alerts
```

### Columns

| Column | Data Type | Constraints | Description |
|----------|-----------|-------------|-------------|
| alert_id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique alert identifier |
| rule_type | VARCHAR(64) | NOT NULL | e.g. `AMOUNT_THRESHOLD` (no FK to a rules table in MVP) |
| account_id | VARCHAR(64) | NOT NULL | Denormalized from triggering transaction(s) for list/filter |
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
| `idx_alert_created` | `(created_at)` | History / ordering |

---

## 3.3 Alert Transactions

### Purpose

Many-to-many link between alerts and the transaction(s) that contributed
to the alert. MVP Amount Threshold uses one row; Velocity uses several.

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

# 4. Entity Relationships

| Parent | Child | Relationship |
|--------|-------|--------------|
| alerts | alert_transactions | One-to-Many |
| transactions | alert_transactions | One-to-Many |

`account_id` / `payee_id` on `transactions` (and `account_id` on
`alerts`) are **not** foreign keys to master tables.

```
transactions (1) ----< alert_transactions >---- (1) alerts
```

---

# 5. Explicitly out of MVP schema

Do **not** add these unless a milestone says so:

| Table / idea | When |
|--------------|------|
| `accounts`, `payees` | Only if the product starts managing account/payee master data |
| `rules`, `rule_parameters` | Configurable / DB-driven rules (post-MVP enhancement) |
| `alert_status_history` | Phase 4 audit trail (who/what/when per transition) |
| users / auth tables | Out of scope — single operator, no auth |

---

# 6. Assumptions

- Transactions arrive via REST API; the system does not onboard accounts.
- Rule evaluation is synchronous in MVP (no queue yet).
- Amount Threshold threshold value is hardcoded in the rule constructor.
- Authentication is outside MVP scope.

---

# 7. Document Information

| Item | Value |
|------|-------|
| Document Name | Database Design Document |
| Project | Transaction Monitoring System |
| Version | 1.1 |
| Database | MySQL (MVP) |
| Status | Aligned with ERD + backend schema decisions |
| Last Updated | 03 August 2026 |
