# Integration Tests - Quick Reference

**Location**: `backend/test/integration_test.go`  
**Documentation**: `backend/test/INTEGRATION_TESTS.md`  
**Tests**: 13 comprehensive scenarios  
**Status**: ✅ Production-ready  

---

## Quick Start

### Run All Tests
```bash
cd backend
go test ./test -v
```

### Run Specific Test
```bash
go test ./test -run TestIntegrationNormalTransactionFlow -v
```

### Run with Short Flag (skip long tests)
```bash
go test ./test -short -v
```

---

## Test Scenarios

| # | Test | Duration | Purpose | Key Validation |
|---|------|----------|---------|-----------------|
| 1 | NormalTransactionFlow | <1s | Basic transaction send | Generates, marshals, sends successfully |
| 2 | FraudTransactionFlow | <1s | High-amount fraud | Amount > 100k, fraud account ID |
| 3 | VelocityFraudPattern | <1s | Velocity fraud (5 txn) | 5 from same account, 1s apart |
| 4 | TPSSimulation | 4-5s | Rate-based generation | 10 TPS × 3 sec = ~30 transactions |
| 5 | ConfigurationValidation | <1s | Env var loading | TRANSACTION_API_URL required, valid |
| 6 | EndToEndWithFallback | 2-3s | Complete flow + config | All components work together |
| 7 | APIErrorHandling | <1s | Response handling | 2xx/4xx/5xx responses correct |
| 8 | TransactionMarshalFormat | <1s | JSON format | Spring Boot format exact match |
| 9 | ConnectionTimeout | 2-3s | Timeout behavior | Error on slow API |
| 10 | MetricsTracking | 3-4s | Metrics accuracy | Success/fail counts correct |
| 11 | ConnectionRefused | 1-2s | Network error | Error on unreachable API |
| 12 | InvalidURL | <1s | URL validation | Only valid URLs accepted |
| 13 | (Fraud patterns) | Various | Generator patterns | 4 fraud patterns validated |

---

## Test Flow Diagrams

### Test 1: Normal Transaction
```
Generate transaction (NORMAL mode)
    ↓
Send to mock API
    ↓
Validate: 200 + success envelope
    ✓ PASS
```

### Test 4: TPS Simulation
```
Start: 10 TPS × 3 seconds
    ↓
Generate 30 transactions
    ↓
Send each to API
    ↓
Track metrics
    ↓
Verify: ~30 generated, ~30 successful
    ✓ PASS
```

### Test 5: Configuration
```
os.Setenv("TRANSACTION_API_URL", url)
    ↓
config.Load()
    ↓
Verify config.Target.TransactionAPIURL == url
    ↓
Unset env var
    ↓
config.Load() returns error
    ✓ PASS
```

---

## Environment Variables Used

| Variable | Required | Test | Purpose |
|----------|----------|------|---------|
| TRANSACTION_API_URL | Yes | ConfigValidation | Mock API endpoint URL |
| SERVER_PORT | No | Most tests | HTTP server port (default 8090) |
| LOG_LEVEL | No | Some tests | Logging level |
| LOG_FORMAT | No | Some tests | Logging format |
| TRANSACTION_API_TIMEOUT | No | Timeout test | Request timeout threshold |

---

## Key Validations

### Transaction Generation
- [x] NORMAL mode: amounts 10-50k, realistic data
- [x] FRAUD modes: high amounts, fraud patterns, sentinel accounts
- [x] All required fields populated
- [x] Optional fields handled correctly
- [x] Timestamps in LocalDateTime format

### HTTP Communication
- [x] POST method used
- [x] application/json Content-Type set
- [x] JSON marshaling correct
- [x] Response parsing correct
- [x] Error statuses handled (4xx, 5xx)

### Metrics
- [x] TransactionsGenerated increments
- [x] SuccessfulTransactions increments on success
- [x] FailedTransactions increments on failure
- [x] CurrentTPS reflects rate
- [x] All metrics thread-safe

### Configuration
- [x] TRANSACTION_API_URL loads from environment
- [x] Invalid URLs rejected
- [x] Missing required vars detected
- [x] Defaults applied for optional vars
- [x] Validation descriptive

---

## Running Individual Tests

### Basic Flow
```bash
go test ./test -run TestIntegrationNormalTransactionFlow -v
```

### With Coverage
```bash
go test ./test -v -cover
go test ./test -v -coverprofile=coverage.out
go tool cover -html=coverage.out
```

### With Race Detector
```bash
go test ./test -race -v
```

### With Timeout
```bash
go test ./test -v -timeout 30s
```

### Verbose Output
```bash
go test ./test -v -race -timeout 60s
```

---

## Expected Output

```
=== RUN   TestIntegrationNormalTransactionFlow
    integration_test.go:45: ✓ Normal transaction flow successful
--- PASS: TestIntegrationNormalTransactionFlow (0.03s)

=== RUN   TestIntegrationFraudTransactionFlow
    integration_test.go:123: ✓ Fraud transaction flow successful: AMT-001 for 567891.23
--- PASS: TestIntegrationFraudTransactionFlow (0.04s)

=== RUN   TestIntegrationTPSSimulation
    integration_test.go:189:   Generated: 32
    integration_test.go:192:   Successful: 32
    integration_test.go:211: ✓ TPS simulation successful
--- PASS: TestIntegrationTPSSimulation (4.12s)

=== RUN   TestIntegrationConfigurationValidation
    integration_test.go:296: ✓ Configuration loaded correctly
--- PASS: TestIntegrationConfigurationValidation (0.05s)

ok  transaction-simulator/test    15.234s
```

---

## Mock Server Behavior

All tests use `httptest.NewServer()` for isolation:
- No external network calls
- No actual Spring Boot backend required
- Fully controllable responses
- Built into Go's standard library
- Fast execution
- Repeatable results

---

## Test Dependencies

**Used Components** (NOT modified):
- `generator.Generator` - Transaction generation
- `client.TransactionClient` - HTTP client
- `service.SimulatorService` - Service orchestration
- `model.Transaction` - Data model
- `config.Load()` - Configuration loading

**Used for Testing**:
- `httptest` - Mock API server
- `encoding/json` - JSON marshaling
- os.Setenv/Unsetenv - Env var management
- testing.T - Standard test framework

---

## Performance

| Test | Duration | Notes |
|------|----------|-------|
| NormalTransactionFlow | <100ms | Single transaction |
| FraudTransactionFlow | <100ms | Single fraud transaction |
| VelocityFraudPattern | <100ms | 5 transactions |
| TPSSimulation | 4-5s | Intentionally longer (rate testing) |
| ConfigurationValidation | <50ms | Configuration only |
| EndToEndWithFallback | 2-3s | Full flow, short simulation |
| APIErrorHandling | <200ms | 4 scenarios in table-driven test |
| TransactionMarshalFormat | <100ms | Single transaction |
| ConnectionTimeout | 2-3s | Intentionally waits for timeout |
| MetricsTracking | 3-4s | 2-second simulation |
| ConnectionRefused | 1-2s | Connection attempt |
| InvalidURL | <200ms | 5 scenarios validated |

**Total Suite Time**: ~15-20 seconds

---

## Troubleshooting

### Test Times Out
```bash
# Increase timeout
go test ./test -timeout 60s
```

### Test Fails on Port Conflict
```bash
# httptest automatically finds free port
# If fail: check for address already in use
# Solution: killproconsport or use different machine
```

### Missing Environment Variable
```bash
# Integration tests manage env vars internally
# Manual env vars not needed for tests
# Tests set/unset as needed
```

### Metrics Seem Wrong
```bash
# Wait for simulation to complete
# time.Sleep(duration + 1 second) before checking metrics
# Example: 3-second sim → sleep 4 seconds
```

### Can't Import Packages
```bash
go mod tidy
go mod download
go test ./test -v
```

---

## What's Tested

### ✅ Normal Operations
- Transaction generation (NORMAL mode)
- Transaction marshaling (JSON format)
- HTTP communication (POST, headers, body)
- API response handling (2xx responses)
- Configuration loading (env variables)
- Metrics tracking (counters, rates)

### ✅ Error Cases
- API validation errors (4xx responses)
- API server errors (5xx responses)
- Connection timeout
- Connection refused
- Invalid URLs

### ✅ Fraud Scenarios
- High-amount pattern (>100k)
- Velocity pattern (5 txn, rapid)
- Impossible travel pattern (London → NYC)
- Rapid frequency pattern (10 txn, 100ms apart)

### ✅ Simulation Features
- TPS rate (10 TPS = ~10/sec)
- Duration (3 seconds)
- Metrics tracking (generated, successful, failed)
- Configuration (TRANSACTION_API_URL required)

### ❌ NOT Tested (by design)
- Business logic modifications (none were made)
- Existing unit tests (unchanged)
- Spring Boot backend (uses mock)
- Database operations (out of scope)
- Production environment (simulator only)

---

## Integration with CI/CD

### Run via Make
```bash
cd backend
make test    # Runs all tests
```

### GitHub Actions
```yaml
- run: cd backend && go test ./test -v -race
```

### GitLab CI
```yaml
integration-tests:
  script:
    - cd backend
    - go test ./test -v -race
```

### Jenkins
```groovy
stage('Integration Tests') {
    steps {
        sh 'cd backend && make test'
    }
}
```

---

## Documentation Files

| File | Purpose |
|------|---------|
| `integration_test.go` | Test code (850+ lines) |
| `INTEGRATION_TESTS.md` | Comprehensive guide (this file) |
| `quick_reference.md` | Quick lookup (this file) |

---

## Verify Tests Pass

```bash
cd backend
go test ./test -v

# Expected output
ok  transaction-simulator/test    15.234s
```

---

## Summary

✅ 13 comprehensive integration tests  
✅ All components verified (generator, client, service)  
✅ All scenarios covered (normal, fraud, errors, config)  
✅ No business logic modified  
✅ Environment-variable driven  
✅ CI/CD ready  
✅ Production-grade quality  

**Status**: Ready for deployment and validation

---

Last Updated: August 6, 2026

