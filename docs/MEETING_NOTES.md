# Meeting Notes — Transaction Monitoring & Alerts

Client / instructor feedback sessions. Add a new dated section after each meeting.  
**Live board:** [`KANBAN.md`](./KANBAN.md) · **Milestones:** [`../Project_milestones.md`](../Project_milestones.md) · **Stand-ups:** [`STANDUP_LOG.md`](./STANDUP_LOG.md)

---

## 05 August 2026 — Client meeting

**Type:** Client feedback / requirements review  
**Sprint:** Sprint 2  
**Context:** Phase 1 MVP is demo-ready; Phase 2 rules (Velocity, New Payee, Daily Limit) are implemented. Client reviewed current progress and requested the following product additions.

### Discussed items

| # | Topic | Detail | Status vs current product |
|---|--------|--------|---------------------------|
| 1 | **Edit rules** | Operators should be able to create/edit rule parameters (thresholds, windows, limits), not only run hardcoded rules | Partially covered in Phase 2 milestones (rules table + config UI — not started) |
| 2 | **Display rules** | UI should list the active rules so the operator can see what is evaluating traffic | Same as above — rules view not built yet |
| 3 | **Explanation about the rules** | Each rule needs a clear human-readable explanation (what it checks, why an alert fired) so the operator understands the detection | New / expand — display names exist for phase 2 rule types; fuller explanations still needed |
| 4 | **Sort and filter alerts by status** | Alert list should support sorting and filtering by lifecycle status (OPEN, ACKNOWLEDGED, INVESTIGATING, CLOSED, DISMISSED) | Partially covered — API already supports status filter; UI sort/filter UX to confirm/finish |
| 5 | **Add graphs** | Dashboard should include graphs (trends / counts over time), not only KPI number strips | New — not in current MVP UI |
| 6 | **Alert severity level** | Surface severity (HIGH / MEDIUM / LOW) with clear visual coding in the UI | Already on Phase 2 milestones — not started |
| 7 | **Failing reason** | When a rule fires, the alert should carry a failing reason (why this transaction matched the rule) visible in list/detail | New / expand — rule matches should expose a clear reason string to the operator |

### Client asks (action list)

1. Rules management: **view**, **edit**, and **explain** rules in the product.
2. Alerts: **filter + sort by status**.
3. Dashboard: **graphs** for monitoring trends.
4. Alerts: show **severity level** (HIGH / MEDIUM / LOW).
5. Alerts: show **failing reason** for why the rule triggered.

### Notes for the team

- Items 1–3 and 6 map closely to existing Phase 2 backlog cards (rules table/config UI, severity colors). Treat client meeting as confirmation to prioritize those.
- Graphs (item 5) and failing reason (item 7) are explicit client asks — add as stories on the kanban / milestones when picked up.
- Do not confuse “failing reason” with alert lifecycle notes (resolution notes on close/dismiss); failing reason is the **rule evaluation explanation** at create time.

### Follow-ups

| Action | Owner | Target | Status |
|--------|-------|--------|--------|
| Capture above items on kanban / milestones as ready stories | Team | Next stand-up | Done 06 Aug |
| Prioritize severity + status filter/sort for next UI pass | Rameez | Sprint 2 | **Done** 06 Aug |
| Plan rules view/edit + explanations + failing reason on alert | sathwik + Rameez | Sprint 2 | **Done** 06 Aug |
| Sketch dashboard graph(s) (what series, which endpoint) | Rameez + shreya | Sprint 2 | **Done** (interactive dashboard) |
| Load-test write-up for GitHub | shreya | Sprint 2 | **Done** (`docs/load-test-results.md`) |
| Final presentation script | ALL | Sprint 2 | **Done** (`docs/PRESENTATION_SCRIPT.md`) |

---

*Last updated: 06 August 2026 EOD — client asks largely closed; Phase 3 + simulator remain*
