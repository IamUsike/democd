package client_test

import (
	"fmt"
	"io"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync/atomic"
	"testing"
	"time"

	"transaction-simulator/client"
	"transaction-simulator/config"
	"transaction-simulator/model"
)

func TestSendTransaction_SuccessfulRequest(t *testing.T) {
	var gotMethod string
	var gotContentType string

	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		gotMethod = r.Method
		gotContentType = r.Header.Get("Content-Type")
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusCreated)
		_, _ = w.Write([]byte(`{"success":true,"message":"Transaction recorded successfully.","data":{}}`))
	}))
	defer server.Close()

	c := mustClient(t, server.URL+"/api/v1/transactions", 250*time.Millisecond, server.Client())

	err := c.SendTransaction(sampleTransaction())
	if err != nil {
		t.Fatalf("SendTransaction returned error: %v", err)
	}
	if gotMethod != http.MethodPost {
		t.Errorf("method: want POST, got %s", gotMethod)
	}
	if gotContentType != "application/json" {
		t.Errorf("content type: want application/json, got %q", gotContentType)
	}
}

func TestSendTransaction_JSONSerialization_UsesModelTransactionSchema(t *testing.T) {
	var body string

	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		payload, err := io.ReadAll(r.Body)
		if err != nil {
			t.Fatalf("failed to read request body: %v", err)
		}
		body = string(payload)
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte(`{"success":true,"message":"ok","data":{}}`))
	}))
	defer server.Close()

	c := mustClient(t, server.URL+"/api/v1/transactions", time.Second, server.Client())

	if err := c.SendTransaction(sampleTransaction()); err != nil {
		t.Fatalf("SendTransaction returned error: %v", err)
	}

	assertContains(t, body, `"sourceType":"BANK"`)
	assertContains(t, body, `"sourceId":"HSBC-UK"`)
	assertContains(t, body, `"sourceName":"HSBC United Kingdom"`)
	assertContains(t, body, `"accountId":"ACC10001"`)
	assertContains(t, body, `"payeeId":"PAYEE5001"`)
	assertContains(t, body, `"payeeName":"Amazon"`)
	assertContains(t, body, `"amount":50000`)
	assertContains(t, body, `"currency":"USD"`)
	assertContains(t, body, `"type":"TRANSFER"`)
	assertContains(t, body, `"timestamp":"2026-08-06T10:30:00"`)
	assertContains(t, body, `"location":"London"`)
	assertContains(t, body, `"latitude":51.5074`)
	assertContains(t, body, `"longitude":-0.1278`)
	assertContains(t, body, `"description":"Online transfer"`)
	assertContains(t, body, `"status":"COMPLETED"`)
}

func TestSendTransaction_InvalidAPIResponse_ReturnsError(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte(`not-json`))
	}))
	defer server.Close()

	c := mustClient(t, server.URL+"/api/v1/transactions", time.Second, server.Client())

	err := c.SendTransaction(sampleTransaction())
	if err == nil {
		t.Fatal("expected error for invalid API response, got nil")
	}
	if !strings.Contains(err.Error(), "invalid API response") {
		t.Errorf("expected invalid API response error, got: %v", err)
	}
}

func TestSendTransaction_ValidationClientError_ReturnsError(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte(`{"timestamp":"2026-08-06T10:40:00Z","status":400,"error":"Bad Request","message":"Amount must be greater than zero.","path":"/api/v1/transactions"}`))
	}))
	defer server.Close()

	c := mustClient(t, server.URL+"/api/v1/transactions", time.Second, server.Client())

	err := c.SendTransaction(sampleTransaction())
	if err == nil {
		t.Fatal("expected client error, got nil")
	}
	if !strings.Contains(err.Error(), "client error 400") {
		t.Errorf("expected client error classification, got: %v", err)
	}
	if !strings.Contains(err.Error(), "Amount must be greater than zero.") {
		t.Errorf("expected backend message in error, got: %v", err)
	}
}

func TestSendTransaction_ServerError_ReturnsError(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte(`{"timestamp":"2026-08-06T10:40:00Z","status":500,"error":"Internal Server Error","message":"Unexpected server error.","path":"/api/v1/transactions"}`))
	}))
	defer server.Close()

	c := mustClient(t, server.URL+"/api/v1/transactions", time.Second, server.Client())

	err := c.SendTransaction(sampleTransaction())
	if err == nil {
		t.Fatal("expected server error, got nil")
	}
	if !strings.Contains(err.Error(), "server error 500") {
		t.Errorf("expected server error classification, got: %v", err)
	}
	if !strings.Contains(err.Error(), "Unexpected server error.") {
		t.Errorf("expected backend message in error, got: %v", err)
	}
}

func TestSendTransaction_TimeoutHandling_ReturnsTimeoutError(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		time.Sleep(150 * time.Millisecond)
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusCreated)
		_, _ = w.Write([]byte(`{"success":true,"message":"ok","data":{}}`))
	}))
	defer server.Close()

	c := mustClient(t, server.URL+"/api/v1/transactions", 50*time.Millisecond, server.Client())

	err := c.SendTransaction(sampleTransaction())
	if err == nil {
		t.Fatal("expected timeout error, got nil")
	}
	if !strings.Contains(strings.ToLower(err.Error()), "timed out") {
		t.Errorf("expected timeout message, got: %v", err)
	}
}

func TestSendTransaction_RespectsConfiguredTimeoutOnProvidedHTTPClient(t *testing.T) {
	var hitCount atomic.Int32

	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		hitCount.Add(1)
		time.Sleep(120 * time.Millisecond)
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusCreated)
		_, _ = w.Write([]byte(`{"success":true,"message":"ok","data":{}}`))
	}))
	defer server.Close()

	providedClient := server.Client()
	providedClient.Timeout = 0

	c := mustClient(t, server.URL+"/api/v1/transactions", 25*time.Millisecond, providedClient)

	err := c.SendTransaction(sampleTransaction())
	if err == nil {
		t.Fatal("expected timeout error, got nil")
	}
	if hitCount.Load() != 1 {
		t.Errorf("expected exactly one request attempt, got %d", hitCount.Load())
	}
}

func TestNewTransactionClient_InvalidURL_ReturnsError(t *testing.T) {
	_, err := client.NewTransactionClient(config.TargetConfig{
		TransactionAPIURL: "://bad-url",
		Timeout:           time.Second,
	}, nil, nil)
	if err == nil {
		t.Fatal("expected constructor error for invalid URL, got nil")
	}
}

func mustClient(t *testing.T, transactionAPIURL string, timeout time.Duration, httpClient *http.Client) *client.TransactionClient {
	t.Helper()
	logger := slog.New(slog.NewTextHandler(io.Discard, nil))
	c, err := client.NewTransactionClient(config.TargetConfig{
		TransactionAPIURL: transactionAPIURL,
		Timeout:           timeout,
	}, httpClient, logger)
	if err != nil {
		t.Fatalf("failed to create client: %v", err)
	}
	return c
}

func sampleTransaction() model.Transaction {
	payeeName := "Amazon"
	location := "London"
	latitude := 51.5074
	longitude := -0.1278
	description := "Online transfer"

	return model.Transaction{
		SourceType:  model.SourceTypeBank,
		SourceID:    "HSBC-UK",
		SourceName:  "HSBC United Kingdom",
		AccountID:   "ACC10001",
		PayeeID:     "PAYEE5001",
		PayeeName:   &payeeName,
		Amount:      50000.00,
		Currency:    "USD",
		Type:        model.TransactionTypeTransfer,
		Timestamp:   model.LocalDateTime{Time: time.Date(2026, 8, 6, 10, 30, 0, 0, time.UTC)},
		Location:    &location,
		Latitude:    &latitude,
		Longitude:   &longitude,
		Description: &description,
		Status:      model.TransactionStatusCompleted,
	}
}

func assertContains(t *testing.T, value, substring string) {
	t.Helper()
	if !strings.Contains(value, substring) {
		t.Fatalf("expected %q to contain %q", value, substring)
	}
}

func TestSendTransaction_InvalidJSONEnvelopeMissingSuccess_ReturnsError(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte(`{"message":"ok but malformed"}`))
	}))
	defer server.Close()

	c := mustClient(t, server.URL+"/api/v1/transactions", time.Second, server.Client())

	err := c.SendTransaction(sampleTransaction())
	if err == nil {
		t.Fatal("expected invalid API response error, got nil")
	}
	if !strings.Contains(err.Error(), "missing success field") {
		t.Errorf("expected missing success field error, got: %v", err)
	}
}

func TestSendTransaction_ResponseBodyFallbackMessage_WhenJSONMissingMessage(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte(`{"error":"Bad Request"}`))
	}))
	defer server.Close()

	c := mustClient(t, server.URL+"/api/v1/transactions", time.Second, server.Client())

	err := c.SendTransaction(sampleTransaction())
	if err == nil {
		t.Fatal("expected client error, got nil")
	}
	if !strings.Contains(err.Error(), "client error 400") {
		t.Errorf("expected client error classification, got: %v", err)
	}
}

func TestSendTransaction_TransportError_ReturnsWrappedError(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {}))
	serverURL := server.URL + "/api/v1/transactions"
	server.Close()

	c := mustClient(t, serverURL, 250*time.Millisecond, &http.Client{})

	err := c.SendTransaction(sampleTransaction())
	if err == nil {
		t.Fatal("expected transport error, got nil")
	}
	if !strings.Contains(err.Error(), "request failed") && !strings.Contains(strings.ToLower(err.Error()), "connect") {
		t.Errorf("expected wrapped transport error, got: %v", err)
	}
}

func TestSendTransaction_UnexpectedStatus_ReturnsError(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusMultipleChoices)
		_, _ = w.Write([]byte("redirected"))
	}))
	defer server.Close()

	c := mustClient(t, server.URL+"/api/v1/transactions", time.Second, server.Client())

	err := c.SendTransaction(sampleTransaction())
	if err == nil {
		t.Fatal("expected unexpected status error, got nil")
	}
	if !strings.Contains(err.Error(), fmt.Sprintf("unexpected response status %d", http.StatusMultipleChoices)) {
		t.Errorf("expected unexpected status classification, got: %v", err)
	}
}

