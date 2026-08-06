package controller

import (
	"encoding/json"
	"log/slog"
	"net/http"

	"transaction-simulator/generator"
	"transaction-simulator/service"
)

// SimulatorController manages simulation lifecycle via REST endpoints.
type SimulatorController struct {
	svc    *service.SimulatorService
	logger *slog.Logger
}

// NewSimulatorController returns a ready-to-use SimulatorController.
func NewSimulatorController(
	svc *service.SimulatorService,
	logger *slog.Logger,
) *SimulatorController {
	if logger == nil {
		logger = slog.Default()
	}
	return &SimulatorController{
		svc:    svc,
		logger: logger,
	}
}

// ── Request/Response DTOs ────────────────────────────────────────────────────

// startSimulationRequest is the JSON body for POST /api/simulator/start.
type startSimulationRequest struct {
	TPS      int    `json:"tps"`
	Duration int    `json:"duration"`
	Mode     string `json:"mode"`
}

// statusResponse is the JSON body for GET /api/simulator/status.
type statusResponse struct {
	Running                bool   `json:"running"`
	TransactionsGenerated  uint64 `json:"transactionsGenerated"`
	SuccessfulTransactions uint64 `json:"successfulTransactions"`
	FailedTransactions     uint64 `json:"failedTransactions"`
	CurrentTPS             int    `json:"currentTPS"`
}

// responseMessage is a generic JSON response body for success/error.
type responseMessage struct {
	Message string `json:"message"`
}

// ── Endpoints ────────────────────────────────────────────────────────────────

// Start begins a new transaction simulation.
//
// POST /api/simulator/start
//
// Request body:
//
//	{
//	  "tps": 1000,
//	  "duration": 300,
//	  "mode": "FRAUD"
//	}
//
// Success (200):
//
//	{
//	  "message": "simulation started"
//	}
//
// Bad request (400): Invalid or missing fields
// Conflict (409): Simulation already running
func (sc *SimulatorController) Start(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")

	var req startSimulationRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sc.logger.Warn("failed to decode start request", "error", err)
		http.Error(w, `{"error":"invalid request body"}`, http.StatusBadRequest)
		return
	}

	// Validate request fields
	if req.TPS <= 0 {
		sc.logger.Warn("invalid start request: tps must be greater than 0", "tps", req.TPS)
		http.Error(w, `{"error":"tps must be greater than 0"}`, http.StatusBadRequest)
		return
	}
	if req.Duration <= 0 {
		sc.logger.Warn("invalid start request: duration must be greater than 0", "duration", req.Duration)
		http.Error(w, `{"error":"duration must be greater than 0"}`, http.StatusBadRequest)
		return
	}
	if req.Mode == "" {
		sc.logger.Warn("invalid start request: mode is required")
		http.Error(w, `{"error":"mode is required"}`, http.StatusBadRequest)
		return
	}

	// Convert mode string to generator.SimulationMode
	mode := generator.SimulationMode(req.Mode)
	if mode != generator.ModeNormal && mode != generator.ModeFraud {
		sc.logger.Warn("invalid start request: unsupported mode", "mode", req.Mode)
		http.Error(w, `{"error":"unsupported mode"}`, http.StatusBadRequest)
		return
	}

	// Call service
	err := sc.svc.Start(service.SimulationRequest{
		TPS:      req.TPS,
		Duration: req.Duration,
		Mode:     mode,
	})
	if err != nil {
		// Check if simulation is already running
		if err.Error() == "simulator service: simulation already running" {
			sc.logger.Warn("start failed: simulation already running")
			w.WriteHeader(http.StatusConflict)
			json.NewEncoder(w).Encode(responseMessage{Message: "simulation already running"})
			return
		}
		sc.logger.Error("start failed", "error", err)
		http.Error(w, `{"error":"internal server error"}`, http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(responseMessage{Message: "simulation started"})
}

// Stop requests a graceful shutdown of the current simulation.
//
// POST /api/simulator/stop
//
// Success (200):
//
//	{
//	  "message": "simulation stopped"
//	}
//
// Error (500): Unexpected server error
func (sc *SimulatorController) Stop(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")

	if err := sc.svc.Stop(); err != nil {
		sc.logger.Error("stop failed", "error", err)
		http.Error(w, `{"error":"internal server error"}`, http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(responseMessage{Message: "simulation stopped"})
}

// Status returns the current or latest simulation metrics.
//
// GET /api/simulator/status
//
// Success (200):
//
//	{
//	  "running": true,
//	  "transactionsGenerated": 50000,
//	  "successfulTransactions": 49950,
//	  "failedTransactions": 50,
//	  "currentTPS": 1000
//	}
func (sc *SimulatorController) Status(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")

	metrics := sc.svc.Metrics()
	resp := statusResponse{
		Running:                metrics.Running,
		TransactionsGenerated:  metrics.TransactionsGenerated,
		SuccessfulTransactions: metrics.SuccessfulTransactions,
		FailedTransactions:     metrics.FailedTransactions,
		CurrentTPS:             metrics.CurrentTPS,
	}

	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(resp)
}

