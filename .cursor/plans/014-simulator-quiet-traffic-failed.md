# Simulator: quiet NORMAL traffic + failed status clarity

## Problem
1. NORMAL continuous traffic (default 50 TPS, ~20 accounts) always trips VELOCITY.
2. Soft tenancy / MVP seed packs claim “no alert” but NEW_PAYEE can fire on cold DB.
3. Fraud mix expands one TPS token into multi-POST patterns → HTTP “failed” spikes.
4. Failed txn % exists but is easy to confuse with fraud mix / HTTP fail metrics.

## Approach
1. **Quiet NORMAL generation** — round-robin synthetic `ACC-QUIET-*` pool sized from TPS vs default velocity (5 / 10 min), stable payee per account, `TRANSFER` type (daily limit only counts DEBIT), amounts under threshold.
2. **Soft tenancy + MVP quiet legs** — fixed known payees; Flyway seed prior history for those account–payee pairs so NEW_PAYEE does not fire; keep over-threshold leg for MVP.
3. **Fraud pacing** — consume one rate token per HTTP POST (multi-leg patterns pay for each leg).
4. **Failed status** — keep `failedPercent`; use proper RNG; clarify UI/About (payload `status: FAILED` vs HTTP OK/failed).

## Out of scope
Disabling rules from the simulator; changing monolith rule evaluation for FAILED txns.
