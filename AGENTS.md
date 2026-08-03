# AGENTS.md — Transaction Monitoring & Alerts Dashboard

Read this before starting or resuming work. See `Project_milestones.md`
for current status — keep it updated as milestones complete.

## What this is
REST API + dashboard recording transactions, evaluating them against
rules, and managing alerts through a lifecycle:
OPEN → ACKNOWLEDGED → INVESTIGATING → CLOSED / DISMISSED.

Stack: Java + Spring Boot (backend), React (frontend), one relational
database. No auth — single operator assumed.

## Architecture: modular monolith
API layer, transaction service, and alert service run as **one Spring
Boot application** — one deployable. They're separate packages with
clean boundaries, not separate services — don't introduce inter-service
HTTP calls or separate deployables between these three.

The **rule engine** is the one piece meant to be extracted and scaled
independently later. Keep it behind a clean interface (`Rule` with
`evaluate(txn, context)`) even while it runs in the same process, so
pulling it out later is a deployment change, not a rewrite. Adding a new
rule type = one new class implementing `Rule`. Never modify the
interface or `RuleEngine` itself to add a rule type.

## Build order — MVP first
1. REST API layer
2. Transaction service (record + query)
3. Rule engine — Amount Threshold rule only, synchronous (no queue yet)
4. Alert service — full lifecycle, not just creation
5. React UI — transaction list, alert list/detail, lifecycle actions

Phase 2+ (don't start early): Velocity / New Payee / Daily Limit rules,
async queue + scaled rule engine workers, caching, read replica, TLS/
encryption hardening. See `Project_milestones.md` for the full breakdown.

## Backend conventions
- Layering: controllers (validation/mapping only) → services (business
  logic) → repositories (Spring Data JPA, no embedded query logic beyond
  derived queries / `@Query`).
- Constructor injection only, no field `@Autowired`.
- Don't return JPA entities from controllers — map to response DTOs.
- Alert lifecycle transitions are validated in the alert service, not
  the controller or the database.
- Migrations via Flyway/Liquibase — no `ddl-auto: update`.

## Frontend conventions
Keep it minimal — this is a single-operator dashboard, not a consumer
product. Functional components + hooks, local state over global state
libraries, a small typed API client per backend module. Build screens in
this priority order: transaction list → active alerts → alert detail →
lifecycle actions → alert history → rules view.

## Testing (TDD)
Write the failing test first, especially for rule engine logic and
lifecycle transitions. JUnit 5 + Mockito for backend; reserve
`@SpringBootTest`/`@WebMvcTest` for the smaller set of true integration
tests.

## Planning
For anything beyond a small fix, produce a short plan before writing
code. Cursor users: save it to `.cursor/plans/`. Everyone else: a short
note in the PR description or a scratch file works fine — the point is
writing the plan down before implementing, not the exact location.

---
*Cursor users: see `.cursor/rules/*.mdc` for finer-grained, auto-scoped
versions of these same conventions (they attach only when you're editing
matching files). This file is the shared baseline for every tool.*
