# Sprint Retrospective — Sprint 1
**Project:** Transaction Monitoring & Alerts Dashboard  
**Sprint dates:** 29 July 2026 – 4 August 2026  
**Retrospective date:** 4 August 2026  
**Format:** Start / Stop / Continue + Action Items  
**Attendees:** sathwik (Person A), shreya (Person B/A), Rameez (Person C/B), chiragtank749

---

## 1. Sprint Goal (was it met?)

> *Stand up the project skeleton, agree the data model, and get the Transaction entity + ingest API working and tested so Person B can wire the rule engine.*

**Verdict: Partially met.**

The skeleton and all design/documentation is in great shape. The Transaction API is implemented and tested — but it lives in the `transaction-api` branch and has **not been merged into `dev`**. Rule engine and alerts work has not started. The sprint goal is ~40% delivered in the shared `dev` branch.

---

## 2. What we shipped (by commit evidence)

| Date | Author | What was done |
|------|--------|---------------|
| 29 Jul | chiragtank749 | Initial Spring Boot project, app properties, branch setup |
| 31 Jul | sathwik | Repo confirmed, skeleton work begins |
| 02 Aug | Rameez | ER diagram created and revised |
| 02 Aug | shreya | API + database design documentation, merged branch conflict |
| 03 Aug | sathwik | Full skeleton replacement (Booking demo → txnmonitor), Vite React frontend placeholder, milestone & docs alignment, package structure (`transaction`, `rule`, `alert`, `common`, `api`) |
| 03 Aug | shreya | Transaction entity, repository, service, controller + `TransactionControllerTest` — 244-line test file — all on `transaction-api` branch |
| 04 Aug | sathwik | DFD-MVP, docs refactor (client feedback), storyline + Kanban board |

### Branches created
| Branch | Owner | Status |
|--------|-------|--------|
| `main` | shared | Stable skeleton |
| `dev` | shared | Working branch — docs + skeleton |
| `transaction-api` | shreya | Transaction API done ✅ — **NOT merged** |
| `feature/new_feature` | ? | Unknown / appears unused |

---

## 3. Milestone status (end of Sprint 1)

| Milestone item | Status | Notes |
|----------------|--------|-------|
| Project skeleton | ✅ Done | Spring Boot + Flyway + React placeholder |
| Transaction entity + repository | ✅ Code done | Stuck in `transaction-api` — needs PR + merge |
| `POST /transactions`, `GET /transactions` | ✅ Code done | Same — not in `dev` |
| Rule engine (`Rule`, `RuleEngine`, `AmountThresholdRule`) | ❌ Not started | No files beyond `package-info.java` |
| Wire recording → rule evaluation | ❌ Not started | Depends on rule engine |
| Alert entity + repository | ❌ Not started | No files beyond `package-info.java` |
| Alert lifecycle endpoints | ❌ Not started | — |
| `GET /alerts` / `GET /alerts/{id}` | ❌ Not started | — |
| React UI (transaction list) | ❌ Placeholder only | Vite scaffold, no real screens |
| React UI (alert list + lifecycle) | ❌ Not started | — |
| KPI strip | ❌ Not started | — |
| Swagger / OpenAPI | ❌ Not started | Dependency added, config stub present |
| Seed / simulate script | ❌ Not started | — |
| End-to-end MVP demo | ❌ Not started | — |

---

## 4. What went well ✅ (Continue doing)

- **Documentation quality is excellent.** ER diagram, DATABASE_DESIGN, DFD, TEAM_WORK_SPLIT, STORYLINE_AND_KANBAN — all produced and aligned to the brief. This is rare and valuable.
- **Architecture decisions were made early and clearly.** Modular monolith, soft tenancy, no auth, synchronous rule evaluation in MVP — all locked and documented before any real code was written.
- **Transaction API had tests written alongside the code.** The `TransactionControllerTest` (244 lines) is a good sign — test-first thinking is present.
- **Package structure is correct from day one.** `transaction`, `rule`, `alert`, `common`, `api` — matches the architecture and AGENTS conventions.
- **Skeleton swap was clean.** Replacing the Booking demo with the txnmonitor skeleton in one commit (`7336641`) was well-executed.
- **ER diagram was reviewed and improved** in response to feedback — the team iterated on design rather than treating it as a one-shot artifact.
- **CI/CD scaffolding exists** — Dockerfile, docker-compose, Jenkinsfile were set up early.

---

## 5. What went wrong ❌ (Stop doing)

- **Work is siloed and not merging.** `transaction-api` branch has working, tested code that nobody else can build on because it hasn't been reviewed and merged. This is the single biggest blocker right now.
- **No pull requests opened.** There is no evidence of PRs being raised for any branch. PRs are how the team reviews, integrates, and unblocks each other — skipping them means branches diverge silently.
- **Branches not following the agreed naming convention.** The kanban in `STORYLINE_AND_KANBAN.md` specifies `feat/A-transactions`, `feat/B-alerts`, etc. Actual branches are `transaction-api` and `feature/new_feature`. Small thing, but it makes it harder to track ownership.
- **Team member names are still placeholders in docs.** `TEAM_WORK_SPLIT.md` and `STORYLINE_AND_KANBAN.md` still say `<Name 1>`, `<Name 2>`, `<Name 3>`. A small but avoidable gap.
- **No sprint planning or stand-up notes were recorded.** We are using Agile but have no sprint log, no velocity reference, and no record of blockers that came up during the week.
- **`feature/new_feature` branch is unexplained.** It shows in `git branch -a` but has no commits visible or any documentation. Should be deleted or described.
- **No retrospective or review scheduled** until now — this is being caught reactively rather than proactively at sprint end.

---

## 6. What to try next sprint 💡 (Start doing)

- **Open a PR for `transaction-api` today** and merge it into `dev` before any other work starts. Rule engine (B) and UI (C) are both blocked until this lands.
- **Run a daily 10-minute stand-up** (even async over chat): *What did I do yesterday? What am I doing today? Am I blocked?* Record a one-line note per person per day in a `docs/STANDUP_LOG.md`.
- **Use the Kanban board actively.** Move cards in `STORYLINE_AND_KANBAN.md` as work starts and finishes. Add the story ID to every commit message and PR title (`A-2: POST transactions ingest`).
- **Enforce WIP limit of 1 per person.** No one starts a new story card until their current one is in Review (PR open).
- **Write failing tests before writing the implementation** for rule engine and alert lifecycle — these are explicitly called out in `AGENTS.md` as TDD areas.
- **Fill in the real team names** in `TEAM_WORK_SPLIT.md` and `STORYLINE_AND_KANBAN.md`.
- **Delete or document `feature/new_feature`** — if it's not used, remove it to reduce confusion.

---

## 7. Action items (owner + due date)

| # | Action | Owner | Due |
|---|--------|-------|-----|
| 1 | Open PR: `transaction-api` → `dev`, review, merge | shreya + sathwik | Today (04 Aug) |
| 2 | Start `Rule` interface + `AmountThresholdRule` with unit tests (story B-1) | Person B (shreya/Rameez) | 05 Aug |
| 3 | Start `Alert` entity + repository (story B-3) — can run in parallel with B-1 | Person B | 05 Aug |
| 4 | Update bluish UI shell / routing (story C-1) | Person C | 05 Aug |
| 5 | Fill in real names in `TEAM_WORK_SPLIT.md` and `STORYLINE_AND_KANBAN.md` | All | Today |
| 6 | Create `docs/STANDUP_LOG.md` and record daily | All | Starting 05 Aug |
| 7 | Delete or document `feature/new_feature` branch | sathwik | Today |
| 8 | Wire `POST /transactions` → rule evaluation once B-1 and B-3 are done (story B-2) | A + B together | 06 Aug |

---

## 8. Sprint 2 goal (proposed)

> *Merge the transaction API, implement the rule engine + Amount Threshold rule (with passing unit tests), create the Alert entity + lifecycle, and wire the ingest → evaluate → alert path end to end so a `POST /transactions` over the threshold creates an `OPEN` alert in the database.*

This is the "spine" of the whole system. Everything else (UI, Swagger, seed script) builds on top of it.

---

## 9. Velocity reference

| Sprint | Stories completed (in `dev`/`main`) | Points |
|--------|--------------------------------------|--------|
| Sprint 1 | Skeleton (ALL) | 3 |
| Sprint 1 | Transaction entity + repo (A-1) — *code done, not merged* | — |
| Sprint 1 | POST/GET /transactions (A-2, A-3) — *code done, not merged* | — |
| **Sprint 2 (actual — days 1-2)** | A-1–A-5, B-1–B-8, C-1–C-6, ALL-1, T-1, T-2, Docker/nginx, Jenkins | ~55+ pts |
| **Sprint 2 (remaining)** | QA/testing (shreya), severity UI, rules config UI, login page | ~10 pts |

---

*Last updated: 05 August 2026 — Sprint 2 velocity vastly exceeded target.*

