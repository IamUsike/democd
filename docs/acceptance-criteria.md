# Acceptance Criteria — Transaction Monitoring & Alerts Dashboard

Given / When / Then. This is the single highest-leverage document for
AI-assisted development: the AI writes materially better code, and far
better tests, when the criteria are explicit.

## TMD-101 — Submit a transaction via the API

AC-101-1 Happy path
Given the API is available
When a client POSTs a transaction with accountId, payeeId, amount,
currency, type and timestamp
Then the transaction is persisted with status COMPLETED
And a unique transaction ID is returned in the response
And the response status is 201 Created

AC-101-2 Missing mandatory field
Given a POST body is missing accountId or amount
When the client submits it
Then the API returns 400 Bad Request with a field-level error message
And no record is created

AC-101-3 Invalid amount
Given the amount is zero, negative, or non-numeric
When the client submits the transaction
Then the API returns 400 Bad Request "Amount must be a positive number"
And no record is created

AC-101-4 Timestamp not supplied
Given the request omits a timestamp
When the transaction is recorded
Then the server assigns the current UTC time
And this is reflected in the stored record

AC-101-5 Rule evaluation is decoupled from recording
Given a transaction is successfully recorded
When the record call returns to the client
Then rule evaluation has been triggered (sync or async per design)
But the 201 response is not blocked waiting on alert generation
to complete, if evaluation is asynchronous

## TMD-201 — Amount threshold rule triggers an alert

AC-201-1  Given an active Amount Threshold rule of $10,000 on account X
When a transaction of $15,000 is recorded on account X
Then an alert is created with status OPEN
And the alert references the triggering transaction and rule
And the alert severity matches the rule's configured severity

AC-201-2  Given the same rule is active
When a transaction of $9,999 is recorded
Then no alert is created

AC-201-3  Given the rule is INACTIVE
When a transaction of $15,000 is recorded
Then no alert is created
And the transaction is still stored normally

AC-201-4  Given a transaction of exactly $10,000 (the threshold)
When it is evaluated
Then the team's documented boundary behaviour is applied
consistently (e.g. "> threshold" excludes the boundary
value — confirm and lock this in code + tests)

## TMD-202 — Velocity rule triggers an alert

AC-202-1  Given a Velocity rule of "more than 5 transactions in 10 minutes"
When a 6th transaction is recorded for account X within the
10-minute window
Then an alert is created referencing all 6 transactions
And the alert is not duplicated on the 7th, 8th... transaction
within the same triggering window (deduplicated, not
re-alerted per transaction)

AC-202-2  Given 5 transactions occurred for account X in the last 10 minutes
When a 6th transaction occurs from a DIFFERENT account
Then no alert is created for account X
And no alert is created for the other account (below threshold)

AC-202-3  Given 6 transactions occurred, but spread across 15 minutes
When the 6th transaction is evaluated
Then no alert is created, since the transactions fall outside
the rolling time window

AC-202-4  Given the time window boundary
When timestamps are compared
Then all comparisons use UTC internally regardless of the
timezone the transaction was submitted with

## TMD-601 — Acknowledge an alert

AC-601-1 Happy path
Given an alert with status OPEN
When the operator acknowledges it
Then the status becomes ACKNOWLEDGED
And an `acknowledged_at` timestamp is recorded
And the status change appears in the alert's history timeline

AC-601-2 Invalid transition
Given an alert with status CLOSED
When the operator attempts to acknowledge it
Then the API returns 409 Conflict "Cannot acknowledge a closed alert"
And the alert status is unchanged

AC-601-3 Already acknowledged
Given an alert with status ACKNOWLEDGED
When the operator acknowledges it again
Then the API returns 409 Conflict, or is idempotent per team decision
(decide and document; recommend 409 to keep lifecycle explicit)

## TMD-603 — Close an alert with resolution notes

AC-603-1 Happy path
Given an alert with status ACKNOWLEDGED or INVESTIGATING
When the operator closes it with resolution notes
Then the status becomes CLOSED
And the resolution notes are stored and displayed on the alert detail
And a `closed_at` timestamp is recorded

AC-603-2 Missing notes
Given the operator attempts to close an alert with an empty notes field
When they submit
Then the action is blocked with "Resolution notes are required to close"
And the alert status is unchanged

AC-603-3 Closing an OPEN alert directly
Given an alert with status OPEN (never acknowledged)
When the operator attempts to close it
Then the team's documented rule applies: either blocked with
"Alert must be acknowledged before closing", or allowed as a
fast-path — decide and document per Appendix E, Testing
Considerations, item 5, then encode consistently

AC-603-4 Reopening a closed alert
Given an alert with status CLOSED
When the operator attempts any lifecycle transition on it
Then the API returns 409 Conflict "Closed alerts cannot be reopened"

## TMD-402 — Alert links to triggering transaction(s)

AC-402-1  Given an alert exists
When the operator views the alert detail
Then all transactions that caused it to trigger are listed
And each shows transaction ID, account, amount, and timestamp
And the rule that fired is named and its parameters shown

AC-402-2  Given a velocity alert triggered by 6 transactions
When the operator views the alert
Then all 6 transactions are listed, not just the 6th
And they are ordered chronologically

AC-402-3  Given the underlying transaction record is later amended
(out of scope for MVP, no update endpoint)
Then this AC is marked N/A until an update endpoint exists —
flag if amendment is added as an enhancement later