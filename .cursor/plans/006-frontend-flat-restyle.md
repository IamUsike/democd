# 006 — Frontend flat restyle (visual pass)

## Goal
Restyle Dashboard / Transactions / Alerts to the flat token system in
`frontend/src/styles/tokens.css`. Layout, data, and functionality unchanged.

## Remove
- Gradient wordmark, blurred/glow BG shapes, glassmorphism
- Hero marketing panel + "Live Monitoring" filler
- Sidebar emoji icons; FRONTEND CONSOLE / Soft Tenancy badges
- KPI gradient top-borders; status colored pills
- Decorated warning/error banners; API base URL in UI

## Replace with
- Flat `--surface` cards/tables, `1px solid var(--border)`, `--radius` ≤ 6px
- Sidebar: plain text; active = 2px left `var(--accent)` only
- Page headers: name + one functional subtitle
- KPIs: uppercase secondary label + 28px mono primary value
- Status: colored dot + text (`--status-open/ack/closed`)
- Mono + tabular-nums on amount / account / txn ID / timestamp cells
- Empty/error: plain `--text-secondary` text

## Files
1. Create `frontend/src/styles/tokens.css`
2. Rewrite `index.css` + `App.css` against tokens
3. `AppShell.tsx` — strip icons/badge/gradient brand
4. `DashboardPage.tsx` — page header instead of hero
5. Pages — plain warning text; tighter subtitles
6. `TransactionsPanel` / `AlertsPanel` / `AlertDetail` — status dots + data cell classes
7. `index.html` — Inter + IBM Plex Mono fonts
