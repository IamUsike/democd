# Simulator REST Controller Implementation

## Summary
Implemented a complete REST controller for the Transaction Simulator with three endpoints, comprehensive tests, and full integration into the application.

## Files Created

### 1. `controller/simulator_controller.go`
REST controller exposing simulation lifecycle management via HTTP endpoints.

**Endpoints:**

- **POST /api/simulator/start**
  - Request: `{"tps": int, "duration": int, "mode": string}`
  - Success (200): `{"message": "simulation started"}`
  - Errors:
    - 400 Bad Request: Invalid TPS, Duration, Mode, or malformed JSON
    - 409 Conflict: Simulation already running

- **POST /api/simulator/stop**
  - Success (200): `{"message": "simulation stopped"}`
  - Errors:
    - 500 Internal Server Error: Unexpected service error

- **GET /api/simulator/status**
  - Success (200): Returns simulation metrics as JSON
  - Response: `{"running": bool, "transactionsGenerated": uint64, "successfulTransactions": uint64, "failedTransactions": uint64, "currentTPS": int}`

**Features:**
- Constructor injection of SimulatorService and Logger
- Comprehensive request validation before calling service
- Proper HTTP status codes and JSON responses
- Structured logging at appropriate levels (WARN, ERROR)
- JSON Content-Type headers on all responses
- Clean separation: controller only handles HTTP concerns, business logic stays in service

### 2. `controller/simulator_controller_test.go`
Comprehensive test suite with 18 test cases covering all scenarios.

**Test Coverage:**

*Start Endpoint:*
- ✓ Valid request with FRAUD mode succeeds
- ✓ Valid request with NORMAL mode succeeds
- ✓ Returns 409 when simulation already running
- ✓ Returns 400 for TPS ≤ 0
- ✓ Returns 400 for Duration ≤ 0
- ✓ Returns 400 for missing mode
- ✓ Returns 400 for invalid mode
- ✓ Returns 400 for malformed JSON

*Stop Endpoint:*
- ✓ Stop succeeds when running
- ✓ Stop succeeds when not running
- ✓ Returns 500 on service error

*Status Endpoint:*
- ✓ Returns current metrics
- ✓ Returns zeros when not running

*HTTP Headers:*
- ✓ All endpoints set correct Content-Type header

**Mock Implementation:**
- Mock SimulatorService tracking calls and results
- Mock error type for error scenarios
- Discard logger to reduce test output

### 3. `cmd/main.go` - Updated
Integrated the simulator controller into the application.

**Changes:**
- Added imports: `client`, `generator`, `service`
- Updated `buildRouter()` to register simulator endpoints
- Added `buildSimulatorController()` factory function that:
  - Loads configuration
  - Creates transaction generator
  - Creates transaction client
  - Creates simulator service
  - Creates and returns controller
- Graceful error handling during initialization

**Endpoint Registration:**
```go
r.Post("/api/simulator/start", simCtrl.Start)
r.Post("/api/simulator/stop", simCtrl.Stop)
r.Get("/api/simulator/status", simCtrl.Status)
```

## Architecture & Design

### Layers
- **Controller (`simulator_controller.go`)**: HTTP request/response handling, validation, error mapping
- **Service (`service/simulator_service.go`)**: Business logic (already existed)
- **Generator & Client**: Transaction generation and sending (already existed)

### Validation Strategy
1. Controller validates request format (JSON, field presence)
2. Controller validates field values (TPS > 0, Duration > 0, Mode valid)
3. Service validates complete request and enforces business rules
4. Service differentiates between validation errors and conflicts

### Error Handling
- **400 Bad Request**: Invalid or missing client input (validation before service call)
- **409 Conflict**: Simulation already running (business rule enforced by service)
- **500 Internal Server Error**: Unexpected service errors
- All errors are logged, conflicts at WARN level, others at ERROR level

### Testing Strategy
- Mock-based unit tests using `httptest.ResponseRecorder` and `httptest.NewRequest`
- No external dependencies or integration tests
- Tests cover happy path, validation errors, business rule conflicts, and edge cases
- Mocks simplify testing by eliminating service/generator/client complexity

## Request/Response Formats

### Start Simulation Request
```json
{
  "tps": 1000,
  "duration": 300,
  "mode": "FRAUD"
}
```

### Simulation Status Response
```json
{
  "running": true,
  "transactionsGenerated": 50000,
  "successfulTransactions": 49950,
  "failedTransactions": 50,
  "currentTPS": 1000
}
```

### Generic Response Messages
```json
{
  "message": "simulation started"
}
```

## Supported Modes
- `NORMAL`: Realistic banking transactions
- `FRAUD`: Transactions with suspicious patterns

## Integration Notes

1. **No Business Logic in Controller**: All validation beyond format checking is deferred to the service
2. **Clean Dependencies**: Controller only depends on service interface and stdlib
3. **Logging**: Uses structured logging with `slog` at appropriate levels
4. **Concurrency Safe**: Delegates to service which handles thread safety
5. **Testability**: Mocks easily substitute for real service during testing

## Running Tests

```bash
cd transaction-simulator/backend
go test ./controller -v
```

## Building & Running

```bash
# Development mode
make run

# Build binary
make build

# Run all tests
make test
```

