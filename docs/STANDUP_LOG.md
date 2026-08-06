# Daily Stand-up Log

Format per entry:
> **[Date] [Name]:** Yesterday · Today · Blocked by?

Keep entries short — one to three lines each. Add a blank line between days.

---

## Sprint 2

### 06 August 2026 — EOD / board refresh

> **shreya:** Yesterday–today: k6 Pass 1–3 + EXPLAIN; wrote `docs/load-test-results.md` + `LOAD_TEST_GUIDE.md`. Today/next: alert simulator; unit-test sweep support. Not blocked (Linux↔Linux :8081 is network/SG — see results doc).

> **sathwik:** Yesterday–today: pagination + `afterId` (stop full-list poll); search-bar fixes; E2E/benchmark support; starting **queue + DB VM migration**. Login/superadmin still open if not merged. Blocked only by Linux↔Linux connectivity for DB split — diagnose with `nc`, not app logs.

> **Rameez:** Yesterday–today: interactive dashboard graphs; rules UI + explanations; severity colours; alert status filter/sort; failing reason in UI; virtual alert scroll; multi-rule alert display; UI theme polish. Next: presentation dry-run with script. Not blocked.

**Checklist snapshot (06 Aug):** interactive dashboard ✅ · edit rules ✅ · rule explanations ✅ · alert status filter/sort ✅ · failing reason ✅ · E2E/benchmarks ✅ · pagination/delta poll ✅ · virtual alert scroll ✅ · multi-rule alerts ✅ · severity ✅ · search fixes ✅ · theme ✅ · “Mark suspicious” ❌ not needed · queue+DB VM 🚧 sathwik · alert simulator 🚧 shreya · final unit tests 🚧 · final presentation → `PRESENTATION_SCRIPT.md`

---

### 06 August 2026 — morning stand-up

> **shreya:** Yesterday: QA/E2E on VM; k6 Pass 1–2 (write ramp + mixed); load-test guide, seed volume, EXPLAIN checks. Today: Pass 3 soak @ 140 VUs; capture `rule.evaluate` after Actuator redeploy; fill `load-test-results.md`. Pair with sathwik on deploy if needed.

> **sathwik:** Yesterday: login + superadmin gate; helped QA branch / Jenkins `BRANCH` tip. Today: finish login; support Actuator redeploy; spike failing-reason field with Rameez. Not blocked.

> **Rameez:** Yesterday: flat restyle polish + coverage follow-ups; walked client severity/filter asks. Today: severity colors + alert status filter/sort in UI. Not blocked.

---

### 05 August 2026

> **shreya:** Yesterday: docker `.env` example; seed script hardening; Swagger pass; merged latest into transaction track. Today: start E2E + rule QA on deployed stack. Not blocked.

> **sathwik:** Yesterday: Phase 2 rules (Velocity, NewPayee, DailyLimit) + unit tests; alert API polish. Today: login page + superadmin. Not blocked.

> **Rameez:** Yesterday: search bar + pause feed (PR #2); AmountThresholdRuleTest fix; coverage docs; frontend flat restyle pass. Today: UI polish + pick next client ask. Not blocked.

*(client meeting — see `MEETING_NOTES.md`; mid-sprint retro — see `SPRINT_RETROSPECTIVE.md` § Sprint 2)*

---

### 04 August 2026

> **shreya:** Landed transaction API into `dev` (A-1–A-3); finished seed script (A-4) + Swagger (A-5); paired on record→evaluate call-site with sathwik; KPI endpoint support for dashboard.

> **sathwik:** Rule engine + AmountThreshold (B-1); alert entity/lifecycle/GET (B-3–B-5); sync wiring (B-2) with shreya; Docker/nginx + Jenkins (PR #1); helped integrate branches into `dev`.

> **Rameez:** UI shell + routing (C-1); transaction list, alert list/detail/lifecycle, KPI strip (C-2–C-4) wired to live APIs; brought `feature/frontend` into the merge; E2E demo dry-run with the team.

*(Sprint 1 close retro — see `SPRINT_RETROSPECTIVE.md`)*

---

## Sprint 1

### 03 August 2026

> **sathwik:** Replaced booking demo with txnmonitor skeleton, package layout (`api`/`transaction`/`rule`/`alert`/`common`), milestone/docs alignment.

> **shreya:** Transaction entity, repository, service, controller + `TransactionControllerTest` on `transaction-api`; database/API design docs with Rameez.

> **Rameez:** ER diagram created and revised; frontend Vite placeholder reviewed for upcoming screens.

---

