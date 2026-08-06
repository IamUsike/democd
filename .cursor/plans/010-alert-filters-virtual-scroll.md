# 010 — Fix alert/txn filters + alerts virtual scroll

## Problems
1. **Alerts virtual scroll broken by CSS** — later `.alerts-list` rule
   (`display: flex; flex: 1; overflow-y: auto` without fixed height)
   overrides `.alerts-list.virtual-list` (`height: min(60vh, 520px)`).
   Scroll happens on the page instead of the list; virtualizer never
   measures a real scroll parent.
2. **Redundant filters** — Alerts and Transactions expose Source ID +
   Account ID *and* Search (`q`). Backend `q` already fuzzy-matches
   accountId / sourceId / sourceName (/ ruleType on alerts).
3. **Exact Source ID / Account ID feel broken** — those fields are
   exact-match and not debounced, so typing "HSBC" returns empty until
   the full id is entered; races with partial keystrokes.

## Fix
1. CSS: keep virtual-list as the scroll container with explicit height;
   drop the conflicting later `.alerts-list` layout override (keep
   alert-item visual tweaks).
2. UI: remove Source ID + Account ID filter inputs from Alerts +
   Transactions; keep Status / Severity / Source Type / Sort / Search.
3. AlertsPanel: `measureElement` for dynamic row heights (multi-rule
   rows); ensure virtual row refs / data-index wired.

## Out of scope
Backend filter API unchanged (sourceId/accountId still accepted if
passed). Infinite scroll. Status filter tabs redesign.
