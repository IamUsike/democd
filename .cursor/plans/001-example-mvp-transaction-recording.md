# Plan: MVP transaction recording (example — delete once you have a real plan)

## Goal
Record a transaction via API and synchronously evaluate it against the
amount threshold rule, per `Project_milestones.md` Phase 1.

## Steps
1. `Transaction` entity + Flyway migration
2. `TransactionRepository` (Spring Data JPA)
3. `TransactionService.record(TransactionRequest): TransactionResponse`
   — persists, then calls `RuleEngine.evaluate(...)`
4. `Rule` interface + `RuleEngine` (iterates active rules)
5. `AmountThresholdRule implements Rule` — hardcoded threshold via
   constructor arg for now
6. `AlertService.create(...)` — called by the rule engine when a rule
   triggers
7. `TransactionController` — `POST /transactions`, `GET /transactions`
8. Tests written before each of 3–6 per `testing.mdc`

## Open questions
- Does `POST /transactions` return the created alert inline, or does the
  client poll `/alerts` separately? — Decision: inline, for a simpler
  demo. Response includes `alert: AlertResponse | null`.
- Idempotency on retry — deferred, noted as a known gap for the
  "what we'd do differently" presentation slide.

## Result
*(fill in once done — what actually shipped, and anything that changed
from the plan above)*
