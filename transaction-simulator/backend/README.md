# transaction-simulator / backend

Independent Go service that generates and submits synthetic bank and merchant
transactions to the Transaction Monitoring API.

## Stack

| Layer | Choice |
|-------|--------|
| Language | Go 1.22 |
| Router | [chi v5](https://github.com/go-chi/chi) |
| Logger | `log/slog` (stdlib, structured JSON) |
| Config | Environment variables + `godotenv` |
| Architecture | Clean — controller → service → generator / client / repository |

## Project layout

```
backend/
├── cmd/           # Application entry point (main.go)
├── config/        # Environment-driven configuration
├── controller/    # HTTP handlers (delivery layer)
├── service/       # Business / simulation logic
├── generator/     # Transaction payload builders
├── client/        # Typed HTTP client for the Monitoring API
├── model/         # Domain types (no framework dependencies)
├── repository/    # Persistence interfaces + implementations
└── utils/         # Shared helpers (logger, etc.)
```

## Quick start

### 1. Install Go 1.22+

<https://go.dev/dl/>

### 2. Copy and edit environment variables

```bash
cp .env.example .env
# adjust SERVER_PORT, MONITORING_API_BASE_URL, etc.
```

### 3. Download dependencies

```bash
go mod tidy
```

### 4. Run

```bash
make run
# or
go run ./cmd
```

Server starts on `http://localhost:8090` by default (override with `SERVER_PORT`).

### 5. Verify

```bash
curl http://localhost:8090/health
```

Expected response:

```json
{
  "status": "UP",
  "service": "transaction-simulator",
  "timestamp": "2026-08-06T10:00:00Z"
}
```

## Running locally

### Run in development mode

```bash
go run ./cmd
```

### Run tests

```bash
go test ./...
```

## Running with Docker

### Build and run the container

```bash
docker compose up --build
```

This will:
1. Build the Docker image (multi-stage: builder → alpine runtime)
2. Start the `transaction-simulator-backend` service
3. Expose port 8080 on your machine
4. Load environment variables from `.env` (if it exists) or `docker-compose.yml`

### Verify the container is running

```bash
curl http://localhost:8080/health
```

### Stop the container

```bash
docker compose down
```

### Build Docker image manually

```bash
docker build -t transaction-simulator:latest .
```

### Run container directly (without compose)

```bash
docker run -d \
  --name simulator \
  -p 8080:8080 \
  -e TRANSACTION_API_URL=http://host.docker.internal:8081/api/v1/transactions \
  -e SERVER_PORT=8080 \
  transaction-simulator:latest
```

## Available make targets

```
make run     — start in development mode
make build   — compile to bin/transaction-simulator
make tidy    — sync go.mod / go.sum
make test    — run all tests
make lint    — run golangci-lint
make clean   — remove build artefacts
```

## Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8090` | HTTP listen port (local default; Docker compose may map `8080`) |
| `SERVER_READ_TIMEOUT` | `10s` | HTTP read timeout |
| `SERVER_WRITE_TIMEOUT` | `10s` | HTTP write timeout |
| `SERVER_IDLE_TIMEOUT` | `60s` | HTTP idle timeout |
| `LOG_LEVEL` | `info` | `debug \| info \| warn \| error` |
| `LOG_FORMAT` | `json` | `json \| text` |
| `TRANSACTION_API_URL` | *(required)* | Full ingest URL, e.g. `http://localhost:8081/api/v1/transactions` |
| `TRANSACTION_API_TIMEOUT` | `30s` | Per-request HTTP timeout |

## Simulator API

### Start — continuous traffic

```bash
curl -sS -X POST http://localhost:8090/api/simulator/start \
  -H 'Content-Type: application/json' \
  -d '{
    "kind": "TRAFFIC",
    "tps": 50,
    "duration": 30,
    "mode": "NORMAL",
    "sourceType": "BANK",
    "fraudMixPercent": 10
  }'
```

- `kind` omitted → `TRAFFIC` (backward compatible with `{tps,duration,mode}`).
- `NORMAL` amounts stay under the default amount threshold (~10k) so quiet traffic does not spam alerts.
- `FRAUD` emits **full** multi-txn sequences (velocity, daily limit, new payee, high amount) — not just the first leg.
- Impossible travel is **not** selected for random FRAUD traffic (no matching rule in the monolith).

### Start — demo scenario pack

```bash
curl -sS -X POST http://localhost:8090/api/simulator/start \
  -H 'Content-Type: application/json' \
  -d '{"kind":"SCENARIO","scenario":"AMOUNT_THRESHOLD"}'
```

| Scenario | What it posts | Expected |
|----------|---------------|----------|
| `AMOUNT_THRESHOLD` | 1 txn over threshold | OPEN — Amount |
| `VELOCITY` | 6 quick same-account txns | OPEN — Velocity |
| `NEW_PAYEE` | 1 txn to a fresh payee id | OPEN — New payee |
| `DAILY_LIMIT` | 6×9000 same account (sum > 50k) | OPEN — Daily limit |
| `SOFT_TENANCY_MIX` | BANK + MERCHANT under threshold | No alert (filter demo) |
| `MVP_SEED` | Port of `scripts/seed-demo.sh` | OPEN — Amount |

### Status / stop

```bash
curl -sS http://localhost:8090/api/simulator/status
curl -sS -X POST http://localhost:8090/api/simulator/stop
```

Status includes `kind`, `scenario`, and `mode` when a run is active or just finished.

## Scope notes

- This service **only ingests transactions**. Alert acknowledge / investigate / close / dismiss stay in the operator dashboard.
- Keep `scripts/seed-demo.sh` for a minimal MVP walkthrough and k6 scripts for load evidence — they are complementary, not replaced.


