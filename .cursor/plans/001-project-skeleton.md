# Plan: Project skeleton (Phase 1 item 1)

## Goal

Ship a runnable empty shell that matches `backend-java.mdc` and the first
Phase 1 milestone: Spring Boot app, package layout, Flyway configured,
React under `frontend/`, and Flyway `V1` creating only the `transactions`
table. **No** Transaction entity/repository, REST endpoints, rule engine,
or alert tables yet.

## Locked decisions

| Decision | Choice |
|----------|--------|
| Base package | `com.example.txnmonitor` |
| Persistence | Spring Data JPA + Flyway; `ddl-auto=validate` |
| DB | MySQL 8; database name `txnmonitor` |
| Artifact | Maven `txnmonitor` |
| Frontend | Vite + React + TypeScript under `frontend/` |

## Steps

1. Retarget `pom.xml` to JPA + Flyway; rename artifact; replace
   `com.example.demo` booking code with `com.example.txnmonitor` modules
   (`api`, `transaction`, `rule`, `alert`, `common`)
2. Wire `application-*.properties` + `docker-compose.yml` for
   `txnmonitor` DB and `ddl-auto=validate`; H2 `test` profile for CI
3. Add `V1__create_transactions.sql` (columns + indexes; no alerts yet)
4. Scaffold `frontend/` Vite React-TS placeholder
5. Update `Project_milestones.md`; root README

## Result

Shipped:
- `com.example.txnmonitor.TxnMonitorApplication` + package-info modules
- Flyway `V1__create_transactions.sql`
- H2-backed `@SpringBootTest` context load (Flyway applies V1)
- `frontend/` placeholder builds successfully
- Booking demo code removed

Next milestone: Transaction entity + repository.
