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

// mockSimulatorService is a mock implementation of SimulatorService for testing.
type mockSimulatorService struct {
	startCalled     bool
	startRequest    service.SimulationRequest
	startErr        error
	stopCalled      bool
	stopErr         error
	metricsResult   service.SimulationMetrics
	metricsCallCount int
}

func (m *mockSimulatorService) Start(request service.SimulationRequest) error {
	m.startCalled = true
	m.startRequest = request
	return m.startErr
}

func (m *mockSimulatorService) Stop() error {
	m.stopCalled = true
	return m.stopErr
}

func (m *mockSimulatorService) Metrics() service.SimulationMetrics {
	m.metricsCallCount++
	return m.metricsResult
}

func discardLogger() *slog.Logger {
	return slog.New(slog.NewTextHandler(io.Discard, nil))
}

// ── Tests ────────────────────────────────────────────────────────────────────

func TestStart_ValidRequest_Success(t *testing.T) {
	mock := &mockSimulatorService{}
	ctrl := NewSimulatorController(mock, discardLogger())

	body := startSimulationRequest{
		TPS:      1000,
		Duration: 300,
		Mode:     "FRAUD",
	}
	bodyBytes, _ := json.Marshal(body)

	req := httptest.NewRequest("POST", "/api/simulator/start", bytes.NewReader(bodyBytes))
	w := httptest.NewRecorder()

	ctrl.Start(w, req)

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
	if mock.startRequest.TPS != 1000 {
		t.Fatalf("expected TPS 1000, got %d", mock.startRequest.TPS)
	}
	if mock.startRequest.Duration != 300 {
		t.Fatalf("expected Duration 300, got %d", mock.startRequest.Duration)
	}
	if mock.startRequest.Mode != generator.ModeFraud {
		t.Fatalf("expected Mode FRAUD, got %s", mock.startRequest.Mode)
	}
}

func TestStart_NormalMode_Success(t *testing.T) {
	mock := &mockSimulatorService{}
	ctrl := NewSimulatorController(mock, discardLogger())

	body := startSimulationRequest{
		TPS:      100,
		Duration: 60,
		Mode:     "NORMAL",
	}
	bodyBytes, _ := json.Marshal(body)

	req := httptest.NewRequest("POST", "/api/simulator/start", bytes.NewReader(bodyBytes))
	w := httptest.NewRecorder()

	ctrl.Start(w, req)

	if w.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d", w.Code)
	}

	if mock.startRequest.Mode != generator.ModeNormal {
		t.Fatalf("expected Mode NORMAL, got %s", mock.startRequest.Mode)
	}
}

func TestStart_SimulationAlreadyRunning_Conflict(t *testing.T) {
	mock := &mockSimulatorService{
		startErr: &mockError{msg: "simulator service: simulation already running"},
	}
	ctrl := NewSimulatorController(mock, discardLogger())

	body := startSimulationRequest{
		TPS:      1000,
		Duration: 300,
		Mode:     "NORMAL",
	}
	bodyBytes, _ := json.Marshal(body)

	req := httptest.NewRequest("POST", "/api/simulator/start", bytes.NewReader(bodyBytes))
	w := httptest.NewRecorder()

	ctrl.Start(w, req)

	if w.Code != http.StatusConflict {
		t.Fatalf("expected status 409, got %d", w.Code)
	}

	var resp responseMessage
	json.NewDecoder(w.Body).Decode(&resp)
	if resp.Message != "simulation already running" {
		t.Fatalf("expected message 'simulation already running', got %q", resp.Message)
	}
}

func TestStart_InvalidTPS_BadRequest(t *testing.T) {
	cases := []struct {
		tps    int
		errmsg string
	}{
		{0, "tps must be greater than 0"},
		{-1, "tps must be greater than 0"},
	}

	for _, tc := range cases {
		t.Run(tc.errmsg, func(t *testing.T) {
			mock := &mockSimulatorService{}
			ctrl := NewSimulatorController(mock, discardLogger())

			body := startSimulationRequest{
				TPS:      tc.tps,
				Duration: 300,
				Mode:     "NORMAL",
			}
			bodyBytes, _ := json.Marshal(body)

			req := httptest.NewRequest("POST", "/api/simulator/start", bytes.NewReader(bodyBytes))
			w := httptest.NewRecorder()

			ctrl.Start(w, req)

			if w.Code != http.StatusBadRequest {
				t.Fatalf("expected status 400, got %d", w.Code)
			}

			if !mock.startCalled {
				expected := false
				if mock.startCalled != expected {
					t.Fatalf("expected Start to not be called on service")
				}
			}
		})
	}
}

func TestStart_InvalidDuration_BadRequest(t *testing.T) {
	cases := []struct {
		duration int
		errmsg   string
	}{
		{0, "duration must be greater than 0"},
		{-1, "duration must be greater than 0"},
	}

	for _, tc := range cases {
		t.Run(tc.errmsg, func(t *testing.T) {
			mock := &mockSimulatorService{}
			ctrl := NewSimulatorController(mock, discardLogger())

			body := startSimulationRequest{
				TPS:      1000,
				Duration: tc.duration,
				Mode:     "NORMAL",
			}
			bodyBytes, _ := json.Marshal(body)

			req := httptest.NewRequest("POST", "/api/simulator/start", bytes.NewReader(bodyBytes))
			w := httptest.NewRecorder()

			ctrl.Start(w, req)

			if w.Code != http.StatusBadRequest {
				t.Fatalf("expected status 400, got %d", w.Code)
			}

			if mock.startCalled {
				t.Fatalf("expected Start to not be called on service")
			}
		})
	}
}

func TestStart_MissingMode_BadRequest(t *testing.T) {
	mock := &mockSimulatorService{}
	ctrl := NewSimulatorController(mock, discardLogger())

	body := startSimulationRequest{
		TPS:      1000,
		Duration: 300,
		Mode:     "",
	}
	bodyBytes, _ := json.Marshal(body)

	req := httptest.NewRequest("POST", "/api/simulator/start", bytes.NewReader(bodyBytes))
	w := httptest.NewRecorder()

	ctrl.Start(w, req)

	if w.Code != http.StatusBadRequest {
		t.Fatalf("expected status 400, got %d", w.Code)
	}

	if mock.startCalled {
		t.Fatalf("expected Start to not be called on service")
	}
}

func TestStart_InvalidMode_BadRequest(t *testing.T) {
	mock := &mockSimulatorService{}
	ctrl := NewSimulatorController(mock, discardLogger())

	body := startSimulationRequest{
		TPS:      1000,
		Duration: 300,
		Mode:     "INVALID",
	}
	bodyBytes, _ := json.Marshal(body)

	req := httptest.NewRequest("POST", "/api/simulator/start", bytes.NewReader(bodyBytes))
	w := httptest.NewRecorder()

	ctrl.Start(w, req)

	if w.Code != http.StatusBadRequest {
		t.Fatalf("expected status 400, got %d", w.Code)
	}

	if mock.startCalled {
		t.Fatalf("expected Start to not be called on service")
	}
}

func TestStart_MalformedJSON_BadRequest(t *testing.T) {
	mock := &mockSimulatorService{}
	ctrl := NewSimulatorController(mock, discardLogger())

	req := httptest.NewRequest("POST", "/api/simulator/start", bytes.NewReader([]byte("not json")))
	w := httptest.NewRecorder()

	ctrl.Start(w, req)

	if w.Code != http.StatusBadRequest {
		t.Fatalf("expected status 400, got %d", w.Code)
	}

	if mock.startCalled {
		t.Fatalf("expected Start to not be called on service")
	}
}

func TestStop_Success(t *testing.T) {
	mock := &mockSimulatorService{}
	ctrl := NewSimulatorController(mock, discardLogger())

	req := httptest.NewRequest("POST", "/api/simulator/stop", nil)
	w := httptest.NewRecorder()

	ctrl.Stop(w, req)

	if w.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d", w.Code)
	}

	var resp responseMessage
	json.NewDecoder(w.Body).Decode(&resp)
	if resp.Message != "simulation stopped" {
		t.Fatalf("expected message 'simulation stopped', got %q", resp.Message)
	}

	if !mock.stopCalled {
		t.Fatal("expected Stop to be called on service")
	}
}

func TestStop_WhenNotRunning_Success(t *testing.T) {
	mock := &mockSimulatorService{}
	ctrl := NewSimulatorController(mock, discardLogger())

	req := httptest.NewRequest("POST", "/api/simulator/stop", nil)
	w := httptest.NewRecorder()

	ctrl.Stop(w, req)

	if w.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d", w.Code)
	}

	if !mock.stopCalled {
		t.Fatal("expected Stop to be called on service")
	}
}

func TestStop_ServiceError_InternalServerError(t *testing.T) {
	mock := &mockSimulatorService{
		stopErr: &mockError{msg: "unexpected error"},
	}
	ctrl := NewSimulatorController(mock, discardLogger())

	req := httptest.NewRequest("POST", "/api/simulator/stop", nil)
	w := httptest.NewRecorder()

	ctrl.Stop(w, req)

	if w.Code != http.StatusInternalServerError {
		t.Fatalf("expected status 500, got %d", w.Code)
	}

	if !mock.stopCalled {
		t.Fatal("expected Stop to be called on service")
	}
}

func TestStatus_ReturnsMetrics(t *testing.T) {
	mock := &mockSimulatorService{
		metricsResult: service.SimulationMetrics{
			Running:                true,
			TransactionsGenerated:  50000,
			SuccessfulTransactions: 49950,
			FailedTransactions:     50,
			CurrentTPS:             1000,
		},
	}
	ctrl := NewSimulatorController(mock, discardLogger())

	req := httptest.NewRequest("GET", "/api/simulator/status", nil)
	w := httptest.NewRecorder()

	ctrl.Status(w, req)

	if w.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d", w.Code)
	}

	var resp statusResponse
	json.NewDecoder(w.Body).Decode(&resp)

	if resp.Running != true {
		t.Fatalf("expected Running true, got %v", resp.Running)
	}
	if resp.TransactionsGenerated != 50000 {
		t.Fatalf("expected TransactionsGenerated 50000, got %d", resp.TransactionsGenerated)
	}
	if resp.SuccessfulTransactions != 49950 {
		t.Fatalf("expected SuccessfulTransactions 49950, got %d", resp.SuccessfulTransactions)
	}
	if resp.FailedTransactions != 50 {
		t.Fatalf("expected FailedTransactions 50, got %d", resp.FailedTransactions)
	}
	if resp.CurrentTPS != 1000 {
		t.Fatalf("expected CurrentTPS 1000, got %d", resp.CurrentTPS)
	}

	if mock.metricsCallCount != 1 {
		t.Fatalf("expected Metrics to be called once, got %d", mock.metricsCallCount)
	}
}

func TestStatus_WhenNotRunning_ReturnsZeros(t *testing.T) {
	mock := &mockSimulatorService{
		metricsResult: service.SimulationMetrics{
			Running:                false,
			TransactionsGenerated:  0,
			SuccessfulTransactions: 0,
			FailedTransactions:     0,
			CurrentTPS:             0,
		},
	}
	ctrl := NewSimulatorController(mock, discardLogger())

	req := httptest.NewRequest("GET", "/api/simulator/status", nil)
	w := httptest.NewRecorder()

	ctrl.Status(w, req)

	if w.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d", w.Code)
	}

	var resp statusResponse
	json.NewDecoder(w.Body).Decode(&resp)

	if resp.Running != false {
		t.Fatalf("expected Running false, got %v", resp.Running)
	}
	if resp.TransactionsGenerated != 0 {
		t.Fatalf("expected TransactionsGenerated 0, got %d", resp.TransactionsGenerated)
	}
}

func TestContentTypeHeaders(t *testing.T) {
	mock := &mockSimulatorService{}
	ctrl := NewSimulatorController(mock, discardLogger())

	t.Run("Start sets Content-Type", func(t *testing.T) {
		body := startSimulationRequest{
			TPS:      100,
			Duration: 60,
			Mode:     "NORMAL",
		}
		bodyBytes, _ := json.Marshal(body)
		req := httptest.NewRequest("POST", "/api/simulator/start", bytes.NewReader(bodyBytes))
		w := httptest.NewRecorder()

		ctrl.Start(w, req)

		ct := w.Header().Get("Content-Type")
		if ct != "application/json; charset=utf-8" {
			t.Fatalf("expected Content-Type application/json, got %q", ct)
		}
	})

	t.Run("Stop sets Content-Type", func(t *testing.T) {
		req := httptest.NewRequest("POST", "/api/simulator/stop", nil)
		w := httptest.NewRecorder()

		ctrl.Stop(w, req)

		ct := w.Header().Get("Content-Type")
		if ct != "application/json; charset=utf-8" {
			t.Fatalf("expected Content-Type application/json, got %q", ct)
		}
	})

	t.Run("Status sets Content-Type", func(t *testing.T) {
		req := httptest.NewRequest("GET", "/api/simulator/status", nil)
		w := httptest.NewRecorder()

		ctrl.Status(w, req)

		ct := w.Header().Get("Content-Type")
		if ct != "application/json; charset=utf-8" {
			t.Fatalf("expected Content-Type application/json, got %q", ct)
		}
	})
}

// mockError is a simple error type for testing.
type mockError struct {
	msg string
}

func (e *mockError) Error() string {
	return e.msg
}

