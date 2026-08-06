# Transaction Simulator Frontend

React + TypeScript + Vite dashboard for the standalone traffic / scenario simulator.

## Install

```bash
npm install
```

## Run

```bash
cp .env.example .env
npm run dev
```

Default API URL: `http://localhost:8090` (`VITE_SIMULATOR_API_URL`).

## What you can do

1. **Demo scenario packs** — one-click sequences that fire Amount / Velocity / New Payee / Daily Limit (or soft-tenancy mix / MVP seed).
2. **Continuous traffic** — TPS + duration + NORMAL/FRAUD, optional BANK/MERCHANT filter and fraud mix %.

Alert lifecycle actions are **not** here — use the main operator dashboard after packs create OPEN alerts.

## Scripts

```bash
npm run typecheck
npm run build
```
