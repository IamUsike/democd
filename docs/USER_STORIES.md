# User Stories — Transaction Monitoring & Alerts Dashboard

**Story format:** As a \<role\>, I want \<capability\>, so that \<benefit\>.  
**Split rule:** If a story cannot be built and demoed in under one day, split it.

**Related:** [`STORYLINE_AND_KANBAN.md`](./STORYLINE_AND_KANBAN.md) (product narrative + team story IDs) · [`KANBAN.md`](./KANBAN.md) (live board) · [`MEETING_NOTES.md`](./MEETING_NOTES.md) · [`Project_milestones.md`](../Project_milestones.md)

**Size key:** S ≈ small · M ≈ medium · L ≈ large  
**FR refs:** map to functional requirements in the project brief.

---

## Epic: Transaction Recording

| ID | Story | Size | FR | Sprint |
|----|-------|------|-----|--------|
| **TMD-101** | As an Operator, I want to submit a transaction via the API, so that it is stored and available for monitoring. | M | FR-TXN-01, FR-TXN-02 | 1 |
| **TMD-102** | As an Operator, I want each transaction to include id, account, amount, and timestamp as the minimum fields, so that the system can evaluate it against rules without unnecessary complexity. | S | FR-TXN-01 | 1 |
| **TMD-103** | As a Developer, I want a data generator/simulator that posts test transactions, so that the team can demo rule triggers without manual entry. | M | FR-TXN-03 | 1 |

---

## Epic: Rule Engine — Amount Threshold

| ID | Story | Size | FR | Sprint |
|----|-------|------|-----|--------|
| **TMD-201** | As an Operator, I want the system to flag any transaction over a configured threshold, so that high-value transactions are reviewed. | M | FR-RUL-01 | 1 |

---

## Epic: Rule Engine — Velocity

| ID | Story | Size | FR | Sprint |
|----|-------|------|-----|--------|
| **TMD-202** | As an Operator, I want the system to flag an account when more than N transactions occur within T minutes, so that rapid-fire activity is caught. | L | FR-RUL-02 | 2 |
| **TMD-203** | As a Developer, I want an efficient query/index on account + timestamp, so that velocity checks stay fast as transaction volume grows. | M | FR-RUL-02 | 2 |

---

## Epic: Rule Engine — New Payee

| ID | Story | Size | FR | Sprint |
|----|-------|------|-----|--------|
| **TMD-204** | As an Operator, I want the system to flag the first transaction to a previously unseen payee, so that unusual counterparties are reviewed. | M | FR-RUL-03 | 2 |

---

## Epic: Rule Engine — Daily Limit (Advanced)

| ID | Story | Size | FR | Sprint |
|----|-------|------|-----|--------|
| **TMD-205** | As an Operator, I want the system to flag an account whose cumulative daily total exceeds a limit, so that structuring/threshold-avoidance patterns are caught. | L | FR-RUL-04 | 3 |

---

## Epic: Rule Management

| ID | Story | Size | FR | Sprint |
|----|-------|------|-----|--------|
| **TMD-301** | As an Operator, I want to view and edit monitoring rules through the UI, so that I can tune detection without redeploying code. | L | FR-RUL-05, FR-RUL-06 | 3 |
| **TMD-302** | As an Operator, I want to activate/deactivate a rule, so that I can disable noisy rules without deleting them. | S | FR-RUL-06 | 3 |

---

## Epic: Alert Generation

| ID | Story | Size | FR | Sprint |
|----|-------|------|-----|--------|
| **TMD-401** | As an Operator, I want an alert automatically created when a rule triggers, so that suspicious activity surfaces without manual checking. | M | FR-ALT-01 | 1 |
| **TMD-402** | As an Operator, I want each alert to link back to its triggering transaction(s) and rule, so that I can understand why it fired. | M | FR-ALT-02 | 2 |
| **TMD-403** | As an Operator, I want alerts assigned a severity (HIGH / MEDIUM / LOW), so that I can prioritize review. | S | FR-ALT-03 | 2 |

---

## Epic: Alerts Dashboard (View)

| ID | Story | Size | FR | Sprint |
|----|-------|------|-----|--------|
| **TMD-501** | As an Operator, I want to view all active alerts in a list, so that I know what needs attention right now. | M | FR-DSH-01 | 2 |
| **TMD-502** | As an Operator, I want to view alert details and the transactions that triggered it, so that I can investigate without switching screens. | M | FR-DSH-02 | 2 |
| **TMD-503** | As an Operator, I want to filter/search transactions by account, date range, and amount, so that I can find specific activity quickly. | M | FR-DSH-03 | 1 |

---

## Epic: Alert Lifecycle

| ID | Story | Size | FR | Sprint |
|----|-------|------|-----|--------|
| **TMD-601** | As an Operator, I want to acknowledge an OPEN alert, so that others know it's being looked at. | S | FR-LFC-01 | 2 |
| **TMD-602** | As an Operator, I want to mark an acknowledged alert as INVESTIGATING, so that its status reflects active work. | S | FR-LFC-02 | 3 |
| **TMD-603** | As an Operator, I want to close an alert with resolution notes, so that there's a record of the outcome. | M | FR-LFC-03 | 3 |
| **TMD-604** | As an Operator, I want to dismiss an alert as a false positive, so that non-issues don't clutter the active queue. | S | FR-LFC-04 | 3 |
| **TMD-605** | As an Operator, I want to view the full alert history and status timeline, so that I can audit how each alert was handled. | M | FR-LFC-05 | 4 |

---

## Epic: Reporting / Audit

| ID | Story | Size | FR | Sprint |
|----|-------|------|-----|--------|
| **TMD-701** | As an Admin, I want to see alert statistics (counts by status, avg time-to-acknowledge), so that I can gauge operator performance and alert quality. | M | FR-RPT-01 | 4 |
| **TMD-702** | As an Admin, I want an audit trail of who changed a rule or alert status and when, so that we meet compliance requirements. | M | FR-RPT-02 | 4 |

---

## Story index by sprint

| Sprint | Story IDs |
|--------|-----------|
| **1** | TMD-101, TMD-102, TMD-103, TMD-201, TMD-401, TMD-503 |
| **2** | TMD-202, TMD-203, TMD-204, TMD-402, TMD-403, TMD-501, TMD-502, TMD-601 |
| **3** | TMD-205, TMD-301, TMD-302, TMD-602, TMD-603, TMD-604 |
| **4** | TMD-605, TMD-701, TMD-702 |

---

## Cross-reference (TMD ↔ team board IDs)

Team kanban / milestones still use A-/B-/C- IDs for delivery tracking. Approximate mapping:

| TMD ID | Team board / notes |
|--------|-------------------|
| TMD-101–103 | A-1–A-4 (ingest + seed) |
| TMD-201 | B-1 (AmountThresholdRule) |
| TMD-202–203 | B-6 VelocityRule + indexes |
| TMD-204 | B-7 NewPayeeRule |
| TMD-205 | B-8 DailyLimitRule |
| TMD-301–302 | Rules table + config UI (Phase 2 Ready) |
| TMD-401–402 | B-2–B-5 (alert create + link) |
| TMD-403 | Severity UI (Phase 2 Ready) |
| TMD-501–502 | C-3 alert list + detail |
| TMD-503 | C-2 txn filters (extend date/amount as needed) |
| TMD-601–604 | B-4 lifecycle endpoints + C-3 UI |
| TMD-605, TMD-701–702 | Phase 4 audit / reporting |

---

*Last updated: 05 August 2026*
