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

Open http://localhost:5173 — **Simulator** and **About** in the top nav.

Default API URL: `http://localhost:8090` (`VITE_SIMULATOR_API_URL`).

## Pages

- **Simulator** — scenario packs, continuous traffic, live metrics strip
- **About** — how to use it, pack vs traffic, monitoring rules & soft tenancy

Alert lifecycle actions are **not** here — use the main operator dashboard after packs create OPEN alerts.

## Scripts

```bash
npm run typecheck
npm run build
```
