# Final presentation script — Transaction Monitoring & Alerts

**Audience:** instructors / stakeholders · **Length:** ~15–20 minutes  
**Date context:** 06 August 2026 · Sprint 2  
**Use with:** live UI + [`load-test-results.md`](./load-test-results.md) + optional Cursor canvas

---

## 0. Opening (30 s)

> We built a **transaction monitoring and alerts dashboard**: ingest bank/merchant traffic, evaluate four rules, triage alerts with a full lifecycle, and prove the system under k6 load. Modular Spring Boot monolith + React operator UI.

---

## 1. Team & ownership (1 min)

| Person | Focus |
|--------|--------|
| **shreya** | Transactions API, seed, Swagger, KPIs, QA/k6, alert simulator |
| **sathwik** | Rules & alerts backend, pagination/delta poll, search fixes, queue + DB VM migration |
| **Rameez** | Operator UI — theme, dashboard graphs, rules UX, severity, filters, virtual scroll, multi-rule alerts |

---

## 2. Problem & MVP path (2 min)

> Banks and merchants push payments. We need **detect → alert → investigate → close** for a single operator.

Live path:

1. Seed / simulator → `POST /transactions`
2. Rules fire → `OPEN` alert (+ failing reason, severity)
3. UI: acknowledge → investigate → close/dismiss

---

## 3. Demo — operator UI (5–6 min)

**Script beat-by-beat:**

1. **Theme / shell** — Dark Obsidian operator shell (Rameez).
2. **Dashboard** — KPI strip + interactive graphs (by type/status/severity/**rule**). Click a KPI → filtered alerts.
3. **Transactions** — paginated list, debounced search, **no full-list poll** (`afterId` delta). Pause/resume feed.
4. **Alerts** — virtual scroll; filter/sort by status; severity colours; failing reason on list/detail.
5. **Multi-rule** — one txn can surface combined rule types / HIGH escalation when multiple rules match.
6. **Rules page** — view + edit thresholds; short explanation of what each rule checks.
7. **Lifecycle** — OPEN → ACKNOWLEDGED → INVESTIGATING → CLOSED (or DISMISSED).

*(Skip “Mark suspicious” wording — not in product.)*

---

## 4. Detection engine (2 min)

Four rules (configurable in UI/API):

| Rule | Idea |
|------|------|
| Amount Threshold | Amount above threshold |
| Velocity | Too many txns in a time window |
| New Payee | First time this payee for the account |
| Daily Limit | Cumulative daily debit over limit |

TDD on rule logic + illegal lifecycle transitions.

---

## 5. Scale evidence (3 min) — show `load-test-results.md`

> Short ramps were fine; **soak** exposed co-located JVM+MySQL.

| Pass | RPS | p95 | Fail |
|------|-----|-----|------|
| Write ramp | 234 | 763 ms | 0% |
| Mixed | 242 | 623 ms | 0% |
| Soak 140 VU / 10m | 214 | **1.13 s** | **0.09%** |

Indexes used (EXPLAIN). Mean `rule.evaluate` rose ~53→126 ms under soak → contention, not missing indexes.

**Next lever:** queue + MySQL on a separate VM (in progress — sathwik). Re-run soak to prove the win.

**Ops aside (if asked about Linux↔Linux :8081):** Windows→Linux works; Linux A→Linux B fails even with `nc` — network/SG, not the app.

---

## 6. What’s done vs left (1 min)

**Done (Sprint 2):** interactive dashboard, rules edit + explanations, alert status filter/sort, failing reason, severity, pagination/delta poll, virtual alert scroll, multi-rule alerts, UI theme, search fixes, E2E/load benchmarks documented.

**In flight:** queue + DB VM migration; alert simulator; final unit-test sweep; login/superadmin if still open.

**Out of scope / deferred:** “Mark suspicious” rename; Phase 4 TLS/masking polish; ML.

---

## 7. Close (30 s)

> Modular monolith, four rules, operator loop proven under load, clear Phase 3 path: **split DB and async evaluate**. Happy to take questions on architecture, k6 numbers, or the live demo.

---

## Backup Q&A

| Question | Answer |
|----------|--------|
| Why not microservices? | Clean packages first; extract **rule engine** later without rewrite. |
| Soft tenancy? | One DB; `sourceType` / `sourceId` / `sourceName` on every row. |
| Auth? | Single-operator assumption; login gate stretch. |
| Where are load numbers? | `docs/load-test-results.md` (GitHub) + Cursor canvas locally. |
