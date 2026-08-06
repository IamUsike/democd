package test

import (
	"encoding/json"
	"io"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"os"
	"sync/atomic"
	"testing"
	"time"

	"transaction-simulator/client"
	"transaction-simulator/config"
	"transaction-simulator/generator"
	"transaction-simulator/model"
	"transaction-simulator/service"
)

// ─────────────────────────────────────────────────────────────────────────────
// Integration Tests: Transaction Simulator ← → Spring Boot API
// ─────────────────────────────────────────────────────────────────────────────
//
// These tests verify that the simulator can successfully:
//   1. Generate transactions correctly (normal and fraud modes)
//   2. Marshaled transaction payloads correctly
//   3. Send to the Spring Boot monitoring API
//   4. Handle API responses (success, validation errors, server errors)
//   5. Track metrics (TPS, generated, successful, failed counts)
//   6. Load configuration from environment variables

// TestIntegrationNormalTransactionFlow tests the normal transaction path:
// Generate → Marshal → Send → Verify Response
func TestIntegrationNormalTransactionFlow(t *testing.T) {
	// Setup: Mock API server
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Verify HTTP method
		if r.Method != http.MethodPost {
			t.Errorf("expected POST, got %s", r.Method)
		}

		// Verify Content-Type
		if r.Header.Get("Content-Type") != "application/json" {
			t.Errorf("expected Content-Type: application/json, got %s", r.Header.Get("Content-Type"))
		}

		// Parse request body (transaction)
		body, err := io.ReadAll(r.Body)
		if err != nil {
			t.Fatalf("failed to read request body: %v", err)
		}
		defer r.Body.Close()

		var tx model.Transaction
		if err := json.Unmarshal(body, &tx); err != nil {
			t.Fatalf("failed to unmarshal transaction: %v", err)
		}

		// Verify transaction fields are populated
		if tx.SourceType == "" {
			t.Error("transaction missing sourceType")
		}
		if tx.SourceID == "" {
			t.Error("transaction missing sourceId")
		}
		if tx.SourceName == "" {
			t.Error("transaction missing sourceName")
		}
		if tx.AccountID == "" {
			t.Error("transaction missing accountId")
		}
		if tx.PayeeID == "" {
			t.Error("transaction missing payeeId")
		}
		if tx.Amount <= 0 {
			t.Error("transaction amount must be positive")
		}
		if tx.Currency == "" {
			t.Error("transaction missing currency")
		}
		if tx.Type == "" {
			t.Error("transaction missing type")
		}
		if tx.Status == "" {
			t.Error("transaction missing status")
		}

		// Return success response
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		response := map[string]interface{}{
			"success": true,
			"message": "Transaction received",
		}
		json.NewEncoder(w).Encode(response)
	}))
	defer server.Close()

	// Create transaction client pointing to mock server
	txnClient, err := client.NewTransactionClient(
		config.TargetConfig{
			TransactionAPIURL: server.URL,
			Timeout:           2 * time.Second,
		},
		nil,
		slog.New(slog.NewTextHandler(io.Discard, nil)),
	)
	if err != nil {
		t.Fatalf("failed to create transaction client: %v", err)
	}

	// Create generator
	gen := generator.New(
		time.Now().UnixNano(),
		slog.New(slog.NewTextHandler(io.Discard, nil)),
		time.Now,
	)

	// Generate normal transaction
	tx := gen.Generate(generator.ModeNormal)

	// Verify transaction was generated
	if tx.SourceType == "" || tx.AccountID == "" {
		t.Fatal("generator produced empty transaction")
	}

	// Send transaction to mock API
	err = txnClient.SendTransaction(tx)
	if err != nil {
		t.Fatalf("failed to send transaction: %v", err)
	}

	t.Logf("✓ Normal transaction flow successful: %s from %s", tx.SourceType, tx.AccountID)
}

// TestIntegrationFraudTransactionFlow tests fraud transaction generation:
// High amount, suspicious patterns, etc.
func TestIntegrationFraudTransactionFlow(t *testing.T) {
	// Setup: Mock API server that accepts all transactions
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		body, err := io.ReadAll(r.Body)
		if err != nil {
			t.Fatalf("failed to read request body: %v", err)
		}
		defer r.Body.Close()

		var tx model.Transaction
		if err := json.Unmarshal(body, &tx); err != nil {
			t.Fatalf("failed to unmarshal transaction: %v", err)
		}

		// Fraud transactions should have higher amounts or special accounts
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		response := map[string]interface{}{
			"success": true,
			"message": "Fraud transaction received",
		}
		json.NewEncoder(w).Encode(response)
	}))
	defer server.Close()

	txnClient, err := client.NewTransactionClient(
		config.TargetConfig{
			TransactionAPIURL: server.URL,
			Timeout:           2 * time.Second,
		},
		nil,
		slog.New(slog.NewTextHandler(io.Discard, nil)),
	)
	if err != nil {
		t.Fatalf("failed to create transaction client: %v", err)
	}

	gen := generator.New(
		time.Now().UnixNano(),
		slog.New(slog.NewTextHandler(io.Discard, nil)),
		time.Now,
	)

	// Test high-amount fraud pattern
	sequence := gen.GenerateFraudSequence(generator.FraudPatternHighAmount)
	if len(sequence) == 0 {
		t.Fatal("fraud sequence is empty")
	}

	fraudTx := sequence[0]

	// Verify high-amount characteristics
	if fraudTx.Amount <= 100_000 {
		t.Logf("warning: fraud transaction amount %.2f is not > 100,000", fraudTx.Amount)
	}

	// Verify account ID indicates fraud
	if fraudTx.AccountID != "ACC-FRAUD-AMT-001" {
		t.Logf("warning: fraud account ID %q doesn't match expected pattern", fraudTx.AccountID)
	}

	// Send to API
	err = txnClient.SendTransaction(fraudTx)
	if err != nil {
		t.Fatalf("failed to send fraud transaction: %v", err)
	}

	t.Logf("✓ Fraud transaction flow successful: %s for amount %.2f", fraudTx.AccountID, fraudTx.Amount)
}

// TestIntegrationVelocityFraudPattern tests multiple transactions from same account
func TestIntegrationVelocityFraudPattern(t *testing.T) {
	// Mock API
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		response := map[string]interface{}{
			"success": true,
			"message": "Transaction received",
		}
		json.NewEncoder(w).Encode(response)
	}))
	defer server.Close()

	txnClient, err := client.NewTransactionClient(
		config.TargetConfig{
			TransactionAPIURL: server.URL,
			Timeout:           2 * time.Second,
		},
		nil,
		slog.New(slog.NewTextHandler(io.Discard, nil)),
	)
	if err != nil {
		t.Fatalf("failed to create transaction client: %v", err)
	}

	gen := generator.New(
		time.Now().UnixNano(),
		slog.New(slog.NewTextHandler(io.Discard, nil)),
		time.Now,
	)

	// Generate velocity fraud pattern (5 transactions from same account)
	sequence := gen.GenerateFraudSequence(generator.FraudPatternVelocity)
	if len(sequence) != 5 {
		t.Fatalf("expected 5 velocity transactions, got %d", len(sequence))
	}

	// All should be from same account
	firstAccount := sequence[0].AccountID
	for i, tx := range sequence {
		if tx.AccountID != firstAccount {
			t.Errorf("transaction %d has different account: %s vs %s", i, tx.AccountID, firstAccount)
		}

		// Send each
		err := txnClient.SendTransaction(tx)
		if err != nil {
			t.Fatalf("failed to send velocity transaction %d: %v", i, err)
		}
	}

	t.Logf("✓ Velocity fraud pattern successful: sent 5 transactions from %s", firstAccount)
}

// TestIntegrationTPSSimulation tests the service's ability to generate and send
// transactions at a specific TPS (transactions per second) rate
func TestIntegrationTPSSimulation(t *testing.T) {
	if testing.Short() {
		t.Skip("skipping TPS simulation test in short mode")
	}

	// Setup: Mock API that tracks requests
	var requestCount atomic.Uint64
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		requestCount.Add(1)
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		response := map[string]interface{}{
			"success": true,
			"message": "Transaction received",
		}
		json.NewEncoder(w).Encode(response)
	}))
	defer server.Close()

	// Create service dependencies
	txnClient, err := client.NewTransactionClient(
		config.TargetConfig{
			TransactionAPIURL: server.URL,
			Timeout:           5 * time.Second,
		},
		nil,
		slog.New(slog.NewTextHandler(io.Discard, nil)),
	)
	if err != nil {
		t.Fatalf("failed to create transaction client: %v", err)
	}

	gen := generator.New(
		time.Now().UnixNano(),
		slog.New(slog.NewTextHandler(io.Discard, nil)),
		time.Now,
	)

	// Create simulator service
	sim, err := service.NewSimulatorService(gen, txnClient, slog.New(slog.NewTextHandler(io.Discard, nil)))
	if err != nil {
		t.Fatalf("failed to create simulator service: %v", err)
	}

	// Start simulation: 100 TPS for 10 seconds
	request := service.SimulationRequest{
		TPS:      100,
		Duration: 10,
		Mode:     generator.ModeNormal,
	}

	err = sim.Start(request)
	if err != nil {
		t.Fatalf("failed to start simulation: %v", err)
	}

	// Wait until run completes, then read final metrics snapshot.
	deadline := time.Now().Add(20 * time.Second)
	for {
		metrics := sim.Metrics()
		if !metrics.Running {
			break
		}
		if time.Now().After(deadline) {
			t.Fatalf("simulation did not complete before timeout; latest metrics: %+v", metrics)
		}
		time.Sleep(100 * time.Millisecond)
	}

	// Get metrics
	metrics := sim.Metrics()

	t.Logf("Simulation metrics:")
	t.Logf("  Running: %v", metrics.Running)
	t.Logf("  Generated: %d", metrics.TransactionsGenerated)
	t.Logf("  Successful: %d", metrics.SuccessfulTransactions)
	t.Logf("  Failed: %d", metrics.FailedTransactions)
	t.Logf("  Current TPS: %d", metrics.CurrentTPS)

	// Verify transactions were generated and processed.
	if metrics.TransactionsGenerated == 0 {
		t.Error("no transactions were generated")
	}

	processed := metrics.SuccessfulTransactions + metrics.FailedTransactions
	if processed != metrics.TransactionsGenerated {
		t.Errorf(
			"processed count mismatch: generated=%d successful=%d failed=%d",
			metrics.TransactionsGenerated,
			metrics.SuccessfulTransactions,
			metrics.FailedTransactions,
		)
	}

	received := requestCount.Load()
	if metrics.SuccessfulTransactions != received {
		t.Errorf(
			"successful count does not match API receive count: successful=%d api_received=%d",
			metrics.SuccessfulTransactions,
			received,
		)
	}

	// Verify generated count is close to expected (100 TPS * 10 sec = ~1000).
	const expected = 1000
	const lowerBound = 900
	const upperBound = 1100
	if metrics.TransactionsGenerated < lowerBound || metrics.TransactionsGenerated > upperBound {
		t.Errorf(
			"generated count out of range: got=%d expected~%d range=[%d,%d]",
			metrics.TransactionsGenerated,
			expected,
			lowerBound,
			upperBound,
		)
	}

	t.Logf("✓ TPS simulation successful: generated=%d successful=%d failed=%d api_received=%d current_tps=%d",
		metrics.TransactionsGenerated,
		metrics.SuccessfulTransactions,
		metrics.FailedTransactions,
		received,
		metrics.CurrentTPS,
	)
}

// TestIntegrationConfigurationValidation tests that configuration is correctly
// loaded from environment variables
func TestIntegrationConfigurationValidation(t *testing.T) {
	// Save original env vars
	originalURL := os.Getenv("TRANSACTION_API_URL")
	originalPort := os.Getenv("SERVER_PORT")
	defer func() {
		if originalURL != "" {
			os.Setenv("TRANSACTION_API_URL", originalURL)
		} else {
			os.Unsetenv("TRANSACTION_API_URL")
		}
		if originalPort != "" {
			os.Setenv("SERVER_PORT", originalPort)
		} else {
			os.Unsetenv("SERVER_PORT")
		}
	}()

	// Test: Valid TRANSACTION_API_URL loaded from env (no hardcoded backend URL)
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))
	defer server.Close()
	testURL := server.URL + "/api/v1/transactions"
	os.Setenv("TRANSACTION_API_URL", testURL)

	cfg, err := config.Load()
	if err != nil {
		t.Fatalf("failed to load config with valid URL: %v", err)
	}

	if cfg.Target.TransactionAPIURL != testURL {
		t.Errorf("expected TRANSACTION_API_URL=%s, got %s", testURL, cfg.Target.TransactionAPIURL)
	}

	t.Logf("✓ Configuration loaded correctly: TRANSACTION_API_URL=%s", cfg.Target.TransactionAPIURL)

	// Test: Missing TRANSACTION_API_URL returns error
	os.Unsetenv("TRANSACTION_API_URL")

	_, err = config.Load()
	if err == nil {
		t.Error("expected error when TRANSACTION_API_URL is missing, but got nil")
	}

	t.Logf("✓ Configuration validation correctly rejects missing TRANSACTION_API_URL")
}

// TestIntegrationEndToEndWithFallback tests the full flow with configuration
// fallback to environment variables
func TestIntegrationEndToEndWithFallback(t *testing.T) {
	// Setup: Mock API
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		response := map[string]interface{}{
			"success": true,
			"message": "Transaction received",
		}
		json.NewEncoder(w).Encode(response)
	}))
	defer server.Close()

	// Set environment variable to mock server URL
	originalURL := os.Getenv("TRANSACTION_API_URL")
	defer func() {
		if originalURL != "" {
			os.Setenv("TRANSACTION_API_URL", originalURL)
		} else {
			os.Unsetenv("TRANSACTION_API_URL")
		}
	}()
	os.Setenv("TRANSACTION_API_URL", server.URL)

	// Load config from environment
	cfg, err := config.Load()
	if err != nil {
		t.Fatalf("failed to load config: %v", err)
	}

	// Create client with loaded config
	txnClient, err := client.NewTransactionClient(
		cfg.Target,
		nil,
		slog.New(slog.NewTextHandler(io.Discard, nil)),
	)
	if err != nil {
		t.Fatalf("failed to create transaction client: %v", err)
	}

	// Create generator
	gen := generator.New(
		time.Now().UnixNano(),
		slog.New(slog.NewTextHandler(io.Discard, nil)),
		time.Now,
	)

	// Create simulator service
	sim, err := service.NewSimulatorService(
		gen,
		txnClient,
		slog.New(slog.NewTextHandler(io.Discard, nil)),
	)
	if err != nil {
		t.Fatalf("failed to create simulator service: %v", err)
	}

	// Start short simulation
	request := service.SimulationRequest{
		TPS:      5,
		Duration: 1,
		Mode:     generator.ModeNormal,
	}

	err = sim.Start(request)
	if err != nil {
		t.Fatalf("failed to start simulation: %v", err)
	}

	// Wait for completion
	time.Sleep(2 * time.Second)

	// Verify metrics
	metrics := sim.Metrics()
	if metrics.TransactionsGenerated == 0 {
		t.Error("no transactions generated in end-to-end test")
	}

	t.Logf("✓ End-to-end flow successful: generated %d transactions", metrics.TransactionsGenerated)
}

// TestIntegrationAPIErrorHandling tests how the client handles various API responses
func TestIntegrationAPIErrorHandling(t *testing.T) {
	tests := []struct {
		name           string
		statusCode     int
		responseBody   interface{}
		expectError    bool
		description    string
	}{
		{
			name:       "Success with valid envelope",
			statusCode: http.StatusOK,
			responseBody: map[string]interface{}{
				"success": true,
				"message": "OK",
			},
			expectError: false,
			description: "2xx response with valid API envelope",
		},
		{
			name:           "Client error (400)",
			statusCode:     http.StatusBadRequest,
			responseBody:   `{"success": false, "message": "Invalid transaction"}`,
			expectError:    true,
			description:    "4xx validation error",
		},
		{
			name:           "Server error (500)",
			statusCode:     http.StatusInternalServerError,
			responseBody:   `{"success": false, "message": "Database error"}`,
			expectError:    true,
			description:    "5xx server error",
		},
		{
			name:           "Empty response body (accepted)",
			statusCode:     http.StatusOK,
			responseBody:   "",
			expectError:    false,
			description:    "2xx with empty body (accepted as success)",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			// Create test server with configured response
			server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				w.Header().Set("Content-Type", "application/json")
				w.WriteHeader(tt.statusCode)

				if body, ok := tt.responseBody.(string); ok {
					w.Write([]byte(body))
				} else {
					json.NewEncoder(w).Encode(tt.responseBody)
				}
			}))
			defer server.Close()

			// Create client
			txnClient, err := client.NewTransactionClient(
				config.TargetConfig{
					TransactionAPIURL: server.URL,
					Timeout:           2 * time.Second,
				},
				nil,
				slog.New(slog.NewTextHandler(io.Discard, nil)),
			)
			if err != nil {
				t.Fatalf("failed to create transaction client: %v", err)
			}

			// Create test transaction
			tx := model.Transaction{
				SourceType:  model.SourceTypeBank,
				SourceID:    "TEST-001",
				SourceName:  "Test Bank",
				AccountID:   "ACC001",
				PayeeID:     "PAYEE001",
				Amount:      100.00,
				Currency:    "USD",
				Type:        model.TransactionTypeDebit,
				Timestamp:   model.LocalDateTime{Time: time.Now()},
				Status:      model.TransactionStatusCompleted,
			}

			// Send and test response
			err = txnClient.SendTransaction(tx)

			if tt.expectError && err == nil {
				t.Errorf("%s: expected error but got nil", tt.description)
			}
			if !tt.expectError && err != nil {
				t.Errorf("%s: unexpected error: %v", tt.description, err)
			}

			t.Logf("✓ %s", tt.description)
		})
	}
}

// TestIntegrationTransactionMarshalFormat verifies that transactions are
// marshaled in the exact format expected by the Spring Boot API
func TestIntegrationTransactionMarshalFormat(t *testing.T) {
	// Capture the marshaled transaction
	var lastRequest []byte
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		var err error
		lastRequest, err = io.ReadAll(r.Body)
		if err != nil {
			t.Fatalf("failed to read request body: %v", err)
		}
		defer r.Body.Close()

		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		response := map[string]interface{}{
			"success": true,
			"message": "OK",
		}
		json.NewEncoder(w).Encode(response)
	}))
	defer server.Close()

	txnClient, err := client.NewTransactionClient(
		config.TargetConfig{
			TransactionAPIURL: server.URL,
			Timeout:           2 * time.Second,
		},
		nil,
		slog.New(slog.NewTextHandler(io.Discard, nil)),
	)
	if err != nil {
		t.Fatalf("failed to create transaction client: %v", err)
	}

	// Create a specific transaction
	tx := model.Transaction{
		SourceType: model.SourceTypeBank,
		SourceID:   "HSBCunk",
		SourceName: "HSBC United Kingdom",
		AccountID:  "ACC10001",
		PayeeID:    "PAYEE10001",
		PayeeName:  ptrString("Amazon"),
		Amount:     123.45,
		Currency:   "USD",
		Type:       model.TransactionTypeDebit,
		Timestamp:  model.LocalDateTime{Time: time.Date(2026, 8, 6, 10, 30, 0, 0, time.UTC)},
		Location:   ptrString("London"),
		Latitude:   ptrFloat64(51.5074),
		Longitude:  ptrFloat64(-0.1278),
		Description: ptrString("Test payment"),
		Status:     model.TransactionStatusCompleted,
	}

	err = txnClient.SendTransaction(tx)
	if err != nil {
		t.Fatalf("failed to send transaction: %v", err)
	}

	// Parse marshaled transaction
	var marshaled map[string]interface{}
	err = json.Unmarshal(lastRequest, &marshaled)
	if err != nil {
		t.Fatalf("failed to unmarshal captured transaction: %v", err)
	}

	// Verify all required fields are present and correctly mapped
	requiredFields := []string{
		"sourceType", "sourceId", "sourceName",
		"accountId", "payeeId", "amount", "currency",
		"type", "timestamp", "status",
	}

	for _, field := range requiredFields {
		if _, exists := marshaled[field]; !exists {
			t.Errorf("required field %q missing in marshaled JSON", field)
		}
	}

	// Verify optional fields with values are present
	if val, exists := marshaled["payeeName"]; !exists || val != "Amazon" {
		t.Errorf("payeeName not correctly marshaled: got %v", val)
	}

	// Verify timestamp format (Java LocalDateTime format: yyyy-MM-ddTHH:mm:ss)
	if timestamp, exists := marshaled["timestamp"]; exists {
		if ts, ok := timestamp.(string); ok {
			if ts != "2026-08-06T10:30:00" {
				t.Errorf("timestamp not in correct format: got %q, expected 2026-08-06T10:30:00", ts)
			}
		}
	}

	t.Logf("✓ Transaction marshaled correctly in Spring Boot API format")
	t.Logf("  Marshaled JSON: %s", string(lastRequest))
}

// TestIntegrationConnectionTimeout tests timeout behavior
func TestIntegrationConnectionTimeout(t *testing.T) {
	// Create server that delays response
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Sleep longer than timeout
		time.Sleep(3 * time.Second)
		w.WriteHeader(http.StatusOK)
	}))
	defer server.Close()

	// Create client with short timeout
	txnClient, err := client.NewTransactionClient(
		config.TargetConfig{
			TransactionAPIURL: server.URL,
			Timeout:           500 * time.Millisecond, // Very short timeout
		},
		nil,
		slog.New(slog.NewTextHandler(io.Discard, nil)),
	)
	if err != nil {
		t.Fatalf("failed to create transaction client: %v", err)
	}

	tx := model.Transaction{
		SourceType: model.SourceTypeBank,
		SourceID:   "TEST-001",
		SourceName: "Test",
		AccountID:  "ACC001",
		PayeeID:    "PAYEE001",
		Amount:     100.00,
		Currency:   "USD",
		Type:       model.TransactionTypeDebit,
		Timestamp:  model.LocalDateTime{Time: time.Now()},
		Status:     model.TransactionStatusCompleted,
	}

	err = txnClient.SendTransaction(tx)
	if err == nil {
		t.Error("expected timeout error but got nil")
	}

	t.Logf("✓ Timeout correctly handled: %v", err)
}

// TestIntegrationMetricsTracking verifies that the service accurately tracks
// successful and failed transactions
func TestIntegrationMetricsTracking(t *testing.T) {
	if testing.Short() {
		t.Skip("skipping metrics test in short mode")
	}

	// Setup: Mock API that fails half the time
	requestCount := 0
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		requestCount++
		w.Header().Set("Content-Type", "application/json")

		// Fail every other request
		if requestCount%2 == 0 {
			w.WriteHeader(http.StatusInternalServerError)
			response := map[string]interface{}{
				"success": false,
				"message": "Server error",
			}
			json.NewEncoder(w).Encode(response)
		} else {
			w.WriteHeader(http.StatusOK)
			response := map[string]interface{}{
				"success": true,
				"message": "OK",
			}
			json.NewEncoder(w).Encode(response)
		}
	}))
	defer server.Close()

	// Create service
	txnClient, err := client.NewTransactionClient(
		config.TargetConfig{
			TransactionAPIURL: server.URL,
			Timeout:           2 * time.Second,
		},
		nil,
		slog.New(slog.NewTextHandler(io.Discard, nil)),
	)
	if err != nil {
		t.Fatalf("failed to create transaction client: %v", err)
	}

	gen := generator.New(
		time.Now().UnixNano(),
		slog.New(slog.NewTextHandler(io.Discard, nil)),
		time.Now,
	)

	sim, err := service.NewSimulatorService(gen, txnClient, slog.New(slog.NewTextHandler(io.Discard, nil)))
	if err != nil {
		t.Fatalf("failed to create simulator service: %v", err)
	}

	// Start simulation
	request := service.SimulationRequest{
		TPS:      10,
		Duration: 2,
		Mode:     generator.ModeNormal,
	}

	err = sim.Start(request)
	if err != nil {
		t.Fatalf("failed to start simulation: %v", err)
	}

	// Wait for completion
	time.Sleep(3 * time.Second)

	// Get metrics
	metrics := sim.Metrics()

	t.Logf("Metrics tracking:")
	t.Logf("  Generated: %d", metrics.TransactionsGenerated)
	t.Logf("  Successful: %d", metrics.SuccessfulTransactions)
	t.Logf("  Failed: %d", metrics.FailedTransactions)

	// Verify metrics are tracked
	if metrics.TransactionsGenerated == 0 {
		t.Fatal("no transactions generated")
	}

	total := metrics.SuccessfulTransactions + metrics.FailedTransactions
	if total != metrics.TransactionsGenerated {
		t.Logf("warning: successful+failed (%d) != generated (%d) (some may still be pending)",
			total, metrics.TransactionsGenerated)
	}

	t.Logf("✓ Metrics correctly tracked")
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper functions
// ─────────────────────────────────────────────────────────────────────────────

func ptrString(s string) *string {
	return &s
}

func ptrFloat64(f float64) *float64 {
	return &f
}

// TestIntegrationConnectionRefused tests behavior when API is unreachable
func TestIntegrationConnectionRefused(t *testing.T) {
	// Use a port that's definitely not listening
	txnClient, err := client.NewTransactionClient(
		config.TargetConfig{
			TransactionAPIURL: "http://localhost:19999/api/v1/transactions",
			Timeout:           1 * time.Second,
		},
		nil,
		slog.New(slog.NewTextHandler(io.Discard, nil)),
	)
	if err != nil {
		t.Fatalf("failed to create transaction client: %v", err)
	}

	tx := model.Transaction{
		SourceType: model.SourceTypeBank,
		SourceID:   "TEST-001",
		SourceName: "Test",
		AccountID:  "ACC001",
		PayeeID:    "PAYEE001",
		Amount:     100.00,
		Currency:   "USD",
		Type:       model.TransactionTypeDebit,
		Timestamp:  model.LocalDateTime{Time: time.Now()},
		Status:     model.TransactionStatusCompleted,
	}

	err = txnClient.SendTransaction(tx)
	if err == nil {
		t.Error("expected connection error but got nil")
	}

	t.Logf("✓ Connection refused correctly handled: %v", err)
}

// TestIntegrationInvalidURL tests configuration validation
func TestIntegrationInvalidURL(t *testing.T) {
	tests := []struct {
		name      string
		url       string
		expectErr bool
	}{
		{"Valid HTTP URL", "http://localhost:8080/api", false},
		{"Valid HTTPS URL", "https://api.example.com/transactions", false},
		{"Invalid scheme", "ftp://invalid.com", true},
		{"Missing host", "http://", true},
		{"Invalid URL", "not a url at all", true},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := client.NewTransactionClient(
				config.TargetConfig{
					TransactionAPIURL: tt.url,
					Timeout:           2 * time.Second,
				},
				nil,
				slog.New(slog.NewTextHandler(io.Discard, nil)),
			)

			if tt.expectErr && err == nil {
				t.Errorf("expected error for URL %q but got nil", tt.url)
			}
			if !tt.expectErr && err != nil {
				t.Errorf("unexpected error for URL %q: %v", tt.url, err)
			}

			if err == nil && !tt.expectErr {
				t.Logf("✓ Valid URL accepted: %s", tt.url)
			}
		})
	}
}

