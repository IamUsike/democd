package controller

import (
	"encoding/json"
	"net/http"
	"time"
)

// HealthController handles liveness / readiness probes.
type HealthController struct{}

// NewHealthController returns a ready-to-use HealthController.
func NewHealthController() *HealthController {
	return &HealthController{}
}

// healthResponse is the JSON body returned by the health endpoint.
type healthResponse struct {
	Status    string `json:"status"`
	Service   string `json:"service"`
	Timestamp string `json:"timestamp"`
}

// Check responds with 200 OK and a JSON health payload when the service is alive.
//
// GET /health
func (h *HealthController) Check(w http.ResponseWriter, r *http.Request) {
	body := healthResponse{
		Status:    "UP",
		Service:   "transaction-simulator",
		Timestamp: time.Now().UTC().Format(time.RFC3339),
	}

	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(http.StatusOK)

	if err := json.NewEncoder(w).Encode(body); err != nil {
		http.Error(w, "failed to encode response", http.StatusInternalServerError)
	}
}

