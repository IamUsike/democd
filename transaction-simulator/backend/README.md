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

Server starts on `http://localhost:8090` by default.

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
| `SERVER_PORT` | `8090` | HTTP listen port |
| `SERVER_READ_TIMEOUT` | `10s` | HTTP read timeout |
| `SERVER_WRITE_TIMEOUT` | `10s` | HTTP write timeout |
| `SERVER_IDLE_TIMEOUT` | `60s` | HTTP idle timeout |
| `LOG_LEVEL` | `info` | `debug \| info \| warn \| error` |
| `LOG_FORMAT` | `json` | `json \| text` |
| `MONITORING_API_BASE_URL` | `http://localhost:8081` | Target monitoring API |
| `MONITORING_API_TIMEOUT` | `30s` | Per-request HTTP timeout |

