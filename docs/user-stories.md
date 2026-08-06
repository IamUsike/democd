## User Stories — Transaction Monitoring & Alerts Dashboard

Story format: As a <role>, I want <capability>, so that <benefit>.
Rule: if a story cannot be built and demoed in under one day, split it.

## Epic: Transaction Recording

TMD-101 As an Operator, I want to submit a transaction via the API,
so that it is stored and available for monitoring.
Size: M | FR-TXN-01,02 | Sprint 1

TMD-102 As an Operator, I want each transaction to include id, account, amount, and timestamp as the minimum fields,
so that the system can evaluate it against rules without unnecessary complexity.
Size: S | FR-TXN-01 | Sprint 1

TMD-103 As a Developer, I want a data generator/simulator that posts test transactions,
so that the team can demo rule triggers without manual entry.
Size: M | FR-TXN-03 | Sprint 1

## Epic: Rule Engine — Amount Threshold

TMD-201 As an Operator, I want the system to flag any transaction over a configured threshold,
so that high-value transactions are reviewed.
Size: M | FR-RUL-01 | Sprint 1

## Epic: Rule Engine — Velocity

TMD-202 As an Operator, I want the system to flag an account when more than N transactions occur within T minutes,
so that rapid-fire activity is caught.
Size: L | FR-RUL-02 | Sprint 2

TMD-203 As a Developer, I want an efficient query/index on account + timestamp,
so that velocity checks stay fast as transaction volume grows.
Size: M | FR-RUL-02 | Sprint 2

## Epic: Rule Engine — New Payee

TMD-204 As an Operator, I want the system to flag the first transaction to a previously unseen payee,
so that unusual counterparties are reviewed.
Size: M | FR-RUL-03 | Sprint 2

## Epic: Rule Engine — Daily Limit (Advanced)

TMD-205 As an Operator, I want the system to flag an account whose cumulative daily total exceeds a limit,
so that structuring/threshold-avoidance patterns are caught.
Size: L | FR-RUL-04 | Sprint 3

## Epic: Rule Management

TMD-301 As an Operator, I want to view and edit monitoring rules through the UI,
so that I can tune detection without redeploying code.
Size: L | FR-RUL-05,06 | Sprint 3

TMD-302 As an Operator, I want to activate/deactivate a rule,
so that I can disable noisy rules without deleting them.
Size: S | FR-RUL-06 | Sprint 3

## Epic: Alert Generation

TMD-401 As an Operator, I want an alert automatically created when a rule triggers,
so that suspicious activity surfaces without manual checking.
Size: M | FR-ALT-01 | Sprint 1

TMD-402 As an Operator, I want each alert to link back to its triggering transaction(s) and rule,
so that I can understand why it fired.
Size: M | FR-ALT-02 | Sprint 2

TMD-403 As an Operator, I want alerts assigned a severity (HIGH/MEDIUM/LOW),
so that I can prioritize review.
Size: S | FR-ALT-03 | Sprint 2

## Epic: Alerts Dashboard (View)

TMD-501 As an Operator, I want to view all active alerts in a list,
so that I know what needs attention right now.
Size: M | FR-DSH-01 | Sprint 2

TMD-502 As an Operator, I want to view alert details and the transactions that triggered it,
so that I can investigate without switching screens.
Size: M | FR-DSH-02 | Sprint 2

TMD-503 As an Operator, I want to filter/search transactions by account, date range, and amount,
so that I can find specific activity quickly.
Size: M | FR-DSH-03 | Sprint 1

## Epic: Alert Lifecycle

TMD-601 As an Operator, I want to acknowledge an OPEN alert,
so that others know it's being looked at.
Size: S | FR-LFC-01 | Sprint 2

TMD-602 As an Operator, I want to mark an acknowledged alert as INVESTIGATING,
so that its status reflects active work.
Size: S | FR-LFC-02 | Sprint 3

TMD-603 As an Operator, I want to close an alert with resolution notes,
so that there's a record of the outcome.
Size: M | FR-LFC-03 | Sprint 3

TMD-604 As an Operator, I want to dismiss an alert as a false positive,
so that non-issues don't clutter the active queue.
Size: S | FR-LFC-04 | Sprint 3

TMD-605 As an Operator, I want to view the full alert history and status timeline,
so that I can audit how each alert was handled.
Size: M | FR-LFC-05 | Sprint 4

## Epic: Reporting / Audit

TMD-701 As an Admin, I want to see alert statistics (counts by status, avg time-to-acknowledge),
so that I can gauge operator performance and alert quality.
Size: M | FR-RPT-01 | Sprint 4

TMD-702 As an Admin, I want an audit trail of who changed a rule or alert status and when,
so that we meet compliance requirements.
Size: M | FR-RPT-02 | Sprint 4