package controller

import (
	"encoding/json"
	"log/slog"
	"net/http"
	"strings"

	"transaction-simulator/generator"
	"transaction-simulator/model"
	"transaction-simulator/service"
)

// SimulatorController manages simulation lifecycle via REST endpoints.
type SimulatorController struct {
	svc    simulatorService
	logger *slog.Logger
}

// simulatorService is the minimal contract used by the controller.
type simulatorService interface {
	Start(request service.SimulationRequest) error
	Stop() error
	Metrics() service.SimulationMetrics
}

// NewSimulatorController returns a ready-to-use SimulatorController.
func NewSimulatorController(
	svc simulatorService,
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
	Kind            string  `json:"kind"`
	TPS             int     `json:"tps"`
	Duration        int     `json:"duration"`
	Mode            string  `json:"mode"`
	Scenario        string  `json:"scenario"`
	SourceType      *string `json:"sourceType"`
	FraudMixPercent *int    `json:"fraudMixPercent"`
}

// statusResponse is the JSON body for GET /api/simulator/status.
type statusResponse struct {
	Running                bool   `json:"running"`
	Kind                   string `json:"kind,omitempty"`
	Scenario               string `json:"scenario,omitempty"`
	Mode                   string `json:"mode,omitempty"`
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

// Start begins a new transaction simulation (TRAFFIC or SCENARIO).
//
// POST /api/simulator/start
func (sc *SimulatorController) Start(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")

	var req startSimulationRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sc.logger.Warn("failed to decode start request", "error", err)
		http.Error(w, `{"error":"invalid request body"}`, http.StatusBadRequest)
		return
	}

	simReq, errMsg := mapStartRequest(req)
	if errMsg != "" {
		sc.logger.Warn("invalid start request", "error", errMsg)
		http.Error(w, `{"error":"`+errMsg+`"}`, http.StatusBadRequest)
		return
	}

	err := sc.svc.Start(simReq)
	if err != nil {
		if err.Error() == "simulator service: simulation already running" {
			sc.logger.Warn("start failed: simulation already running")
			w.WriteHeader(http.StatusConflict)
			_ = json.NewEncoder(w).Encode(responseMessage{Message: "simulation already running"})
			return
		}
		// Surface validation errors from the service as 400.
		if strings.HasPrefix(err.Error(), "simulator service:") {
			msg := strings.TrimPrefix(err.Error(), "simulator service: ")
			sc.logger.Warn("start validation failed", "error", err)
			http.Error(w, `{"error":"`+msg+`"}`, http.StatusBadRequest)
			return
		}
		sc.logger.Error("start failed", "error", err)
		http.Error(w, `{"error":"internal server error"}`, http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	_ = json.NewEncoder(w).Encode(responseMessage{Message: "simulation started"})
}

func mapStartRequest(req startSimulationRequest) (service.SimulationRequest, string) {
	kind := generator.SimulationKind(strings.ToUpper(strings.TrimSpace(req.Kind)))
	if kind == "" {
		kind = generator.KindTraffic
	}

	out := service.SimulationRequest{
		Kind:            kind,
		TPS:             req.TPS,
		Duration:        req.Duration,
		FraudMixPercent: req.FraudMixPercent,
	}

	if req.SourceType != nil {
		st := model.SourceType(strings.ToUpper(strings.TrimSpace(*req.SourceType)))
		if st != model.SourceTypeBank && st != model.SourceTypeMerchant {
			return service.SimulationRequest{}, "unsupported sourceType"
		}
		out.SourceType = &st
	}

	switch kind {
	case generator.KindScenario:
		scenario := generator.ScenarioID(strings.ToUpper(strings.TrimSpace(req.Scenario)))
		if scenario == "" {
			return service.SimulationRequest{}, "scenario is required"
		}
		if !generator.IsValidScenario(scenario) {
			return service.SimulationRequest{}, "unsupported scenario"
		}
		out.Scenario = scenario
		return out, ""

	case generator.KindTraffic:
		if req.TPS <= 0 {
			return service.SimulationRequest{}, "tps must be greater than 0"
		}
		if req.Duration <= 0 {
			return service.SimulationRequest{}, "duration must be greater than 0"
		}
		if req.Mode == "" {
			return service.SimulationRequest{}, "mode is required"
		}
		mode := generator.SimulationMode(strings.ToUpper(strings.TrimSpace(req.Mode)))
		if mode != generator.ModeNormal && mode != generator.ModeFraud {
			return service.SimulationRequest{}, "unsupported mode"
		}
		out.Mode = mode
		if req.FraudMixPercent != nil {
			p := *req.FraudMixPercent
			if p < 0 || p > 100 {
				return service.SimulationRequest{}, "fraudMixPercent must be between 0 and 100"
			}
		}
		return out, ""

	default:
		return service.SimulationRequest{}, "unsupported kind"
	}
}

// Stop requests a graceful shutdown of the current simulation.
func (sc *SimulatorController) Stop(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")

	if err := sc.svc.Stop(); err != nil {
		sc.logger.Error("stop failed", "error", err)
		http.Error(w, `{"error":"internal server error"}`, http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	_ = json.NewEncoder(w).Encode(responseMessage{Message: "simulation stopped"})
}

// Status returns the current or latest simulation metrics.
func (sc *SimulatorController) Status(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")

	metrics := sc.svc.Metrics()
	resp := statusResponse{
		Running:                metrics.Running,
		Kind:                   metrics.Kind,
		Scenario:               metrics.Scenario,
		Mode:                   metrics.Mode,
		TransactionsGenerated:  metrics.TransactionsGenerated,
		SuccessfulTransactions: metrics.SuccessfulTransactions,
		FailedTransactions:     metrics.FailedTransactions,
		CurrentTPS:             metrics.CurrentTPS,
	}

	w.WriteHeader(http.StatusOK)
	_ = json.NewEncoder(w).Encode(resp)
}
