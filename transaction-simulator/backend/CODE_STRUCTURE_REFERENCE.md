# Simulator REST Controller - Code Structure Reference

## File: `controller/simulator_controller.go` (188 lines)

### Packages & Imports
```go
package controller

import (
    "encoding/json"
    "log/slog"
    "net/http"
    "transaction-simulator/generator"
    "transaction-simulator/service"
)
```

### Type Definitions

#### SimulatorController
```go
type SimulatorController struct {
    svc    *service.SimulatorService
    logger *slog.Logger
}
```

#### Request DTOs
```go
type startSimulationRequest struct {
    TPS      int    `json:"tps"`
    Duration int    `json:"duration"`
    Mode     string `json:"mode"`
}
```

#### Response DTOs
```go
type statusResponse struct {
    Running                bool   `json:"running"`
    TransactionsGenerated  uint64 `json:"transactionsGenerated"`
    SuccessfulTransactions uint64 `json:"successfulTransactions"`
    FailedTransactions     uint64 `json:"failedTransactions"`
    CurrentTPS             int    `json:"currentTPS"`
}

type responseMessage struct {
    Message string `json:"message"`
}
```

### Public Functions

#### Constructor
```go
func NewSimulatorController(
    svc *service.SimulatorService,
    logger *slog.Logger,
) *SimulatorController
```

#### HTTP Handlers
```go
func (sc *SimulatorController) Start(w http.ResponseWriter, r *http.Request)
func (sc *SimulatorController) Stop(w http.ResponseWriter, r *http.Request)
func (sc *SimulatorController) Status(w http.ResponseWriter, r *http.Request)
```

### Handler Signatures
- `Start(w http.ResponseWriter, r *http.Request)` - POST /api/simulator/start
- `Stop(w http.ResponseWriter, r *http.Request)` - POST /api/simulator/stop
- `Status(w http.ResponseWriter, r *http.Request)` - GET /api/simulator/status

---

## File: `controller/simulator_controller_test.go` (483 lines)

### Packages & Imports
```go
package controller

import (
    "bytes"
    "encoding/json"
    "io"
    "log/slog"
    "net/http"
    "net/http/httptest"
    "testing"
    "transaction-simulator/generator"
    "transaction-simulator/service"
)
```

### Mock Implementation

#### MockSimulatorService
```go
type mockSimulatorService struct {
    startCalled      bool
    startRequest     service.SimulationRequest
    startErr         error
    stopCalled       bool
    stopErr          error
    metricsResult    service.SimulationMetrics
    metricsCallCount int
}

// Implements service.SimulatorService interface
func (m *mockSimulatorService) Start(request service.SimulationRequest) error
func (m *mockSimulatorService) Stop() error
func (m *mockSimulatorService) Metrics() service.SimulationMetrics
```

#### MockError
```go
type mockError struct {
    msg string
}

func (e *mockError) Error() string
```

### Helper Functions
```go
func discardLogger() *slog.Logger  // Returns logger that discards output
```

### Test Cases (18 total)

#### Start Endpoint Tests
1. `TestStart_ValidRequest_Success`
2. `TestStart_NormalMode_Success`
3. `TestStart_SimulationAlreadyRunning_Conflict`
4. `TestStart_InvalidTPS_BadRequest`
5. `TestStart_InvalidDuration_BadRequest`
6. `TestStart_MissingMode_BadRequest`
7. `TestStart_InvalidMode_BadRequest`
8. `TestStart_MalformedJSON_BadRequest`

#### Stop Endpoint Tests
9. `TestStop_Success`
10. `TestStop_WhenNotRunning_Success`
11. `TestStop_ServiceError_InternalServerError`

#### Status Endpoint Tests
12. `TestStatus_ReturnsMetrics`
13. `TestStatus_WhenNotRunning_ReturnsZeros`

#### Cross-Endpoint Tests
14. `TestContentTypeHeaders` (3 sub-tests)
    - Start sets Content-Type
    - Stop sets Content-Type
    - Status sets Content-Type

### Test Pattern
Each test follows:
1. Create mock with expected behavior/error
2. Create controller with mock
3. Create HTTP request/recorder using httptest
4. Call handler method
5. Assert HTTP status code
6. Assert response body
7. Assert mock was called appropriately

---

## File: `cmd/main.go` - Updates (145 lines total)

### Added Imports
```go
import (
    "transaction-simulator/client"
    "transaction-simulator/generator"
    "transaction-simulator/service"
    // ... existing imports
)
```

### Updated buildRouter()
```go
func buildRouter() chi.Router {
    r := chi.NewRouter()
    
    // ... middleware setup ...
    
    // Health endpoint (existing)
    healthCtrl := controller.NewHealthController()
    r.Get("/health", healthCtrl.Check)
    
    // Simulator endpoints (NEW)
    simCtrl, err := buildSimulatorController()
    if err != nil {
        slog.Error("failed to build simulator controller", "error", err)
    } else {
        r.Post("/api/simulator/start", simCtrl.Start)
        r.Post("/api/simulator/stop", simCtrl.Stop)
        r.Get("/api/simulator/status", simCtrl.Status)
    }
    
    return r
}
```

### New Function: buildSimulatorController()
```go
func buildSimulatorController() (*controller.SimulatorController, error) {
    // 1. Load configuration
    cfg, err := config.Load()
    if err != nil {
        return nil, err
    }
    
    // 2. Get logger
    logger := slog.Default()
    
    // 3. Create generator
    gen := generator.New(time.Now().UnixNano(), logger, time.Now)
    
    // 4. Create transaction client
    txnClient, err := client.NewTransactionClient(cfg.Target, nil, logger)
    if err != nil {
        return nil, err
    }
    
    // 5. Create simulator service
    simService, err := service.NewWithComponents(gen, txnClient, logger)
    if err != nil {
        return nil, err
    }
    
    // 6. Create and return controller
    return controller.NewSimulatorController(simService, logger), nil
}
```

---

## HTTP Request/Response Examples

### Example 1: Start Simulation (Success)
```
REQUEST:
POST /api/simulator/start HTTP/1.1
Content-Type: application/json

{
  "tps": 1000,
  "duration": 300,
  "mode": "FRAUD"
}

RESPONSE: 200 OK
Content-Type: application/json; charset=utf-8

{
  "message": "simulation started"
}
```

### Example 2: Start When Already Running
```
REQUEST:
POST /api/simulator/start HTTP/1.1
Content-Type: application/json

{
  "tps": 100,
  "duration": 60,
  "mode": "NORMAL"
}

RESPONSE: 409 Conflict
Content-Type: application/json; charset=utf-8

{
  "message": "simulation already running"
}
```

### Example 3: Invalid TPS Value
```
REQUEST:
POST /api/simulator/start HTTP/1.1
Content-Type: application/json

{
  "tps": 0,
  "duration": 60,
  "mode": "NORMAL"
}

RESPONSE: 400 Bad Request

{
  "error": "tps must be greater than 0"
}
```

### Example 4: Get Status
```
REQUEST:
GET /api/simulator/status HTTP/1.1

RESPONSE: 200 OK
Content-Type: application/json; charset=utf-8

{
  "running": true,
  "transactionsGenerated": 50000,
  "successfulTransactions": 49950,
  "failedTransactions": 50,
  "currentTPS": 1000
}
```

### Example 5: Stop Simulation
```
REQUEST:
POST /api/simulator/stop HTTP/1.1

RESPONSE: 200 OK
Content-Type: application/json; charset=utf-8

{
  "message": "simulation stopped"
}
```

---

## Validation Logic Flow

### Start Endpoint Validation
```
1. JSON Decode ──► Parse request body
   └─ Error ──► 400 Bad Request

2. TPS Validation ──► TPS > 0
   └─ TPS <= 0 ──► 400 Bad Request

3. Duration Validation ──► Duration > 0
   └─ Duration <= 0 ──► 400 Bad Request

4. Mode Present ──► Mode != ""
   └─ Empty ──► 400 Bad Request

5. Mode Valid ──► Mode in {NORMAL, FRAUD}
   └─ Invalid ──► 400 Bad Request

6. Service Call ──► svc.Start()
   ├─ "already running" ──► 409 Conflict
   └─ Other error ──► 500 Internal Server Error

7. Success ──► 200 OK
```

---

## Testing Strategy

### Test Organization
- Mock types (mockSimulatorService, mockError)
- Helper functions (discardLogger)
- Test functions (18 total)

### Test Execution
```go
// Example test pattern
func TestStart_ValidRequest_Success(t *testing.T) {
    // 1. Setup: Create mock with expected behavior
    mock := &mockSimulatorService{}
    ctrl := NewSimulatorController(mock, discardLogger())
    
    // 2. Prepare: Build request
    body := startSimulationRequest{TPS: 1000, Duration: 300, Mode: "FRAUD"}
    bodyBytes, _ := json.Marshal(body)
    
    // 3. Execute: Call handler
    req := httptest.NewRequest("POST", "/api/simulator/start", 
        bytes.NewReader(bodyBytes))
    w := httptest.NewRecorder()
    ctrl.Start(w, req)
    
    // 4. Assert: Verify results
    if w.Code != http.StatusOK {
        t.Fatalf("expected status 200, got %d", w.Code)
    }
    var resp responseMessage
    json.NewDecoder(w.Body).Decode(&resp)
    if resp.Message != "simulation started" {
        t.Fatalf("expected message 'simulation started', got %q", resp.Message)
    }
    if !mock.startCalled {
        t.Fatal("expected Start to be called on service")
    }
}
```

---

## Logging Details

### Logging Levels Used

**WARN Level:**
- Validation failures (bad TPS, bad mode, etc.)
- Conflict conditions (simulation already running)
- Expected but non-critical issues

**ERROR Level:**
- Service errors during Start
- Service errors during Stop
- Unexpected failures

**Structured Logging:**
- All log messages include context as key-value pairs
- Example: `"failed to decode start request", "error", err`
- Example: `"invalid start request: tps must be greater than 0", "tps", req.TPS`

---

## Constants & Magic Values

### HTTP Status Codes
- `200 OK` - Successful operations
- `400 Bad Request` - Validation errors
- `409 Conflict` - Simulation already running
- `500 Internal Server Error` - Unexpected service errors

### Supported Modes
- `"NORMAL"` - Realistic banking transactions
- `"FRAUD"` - Suspicious pattern transactions


