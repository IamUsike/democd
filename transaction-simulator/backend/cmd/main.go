package main

import (
	"context"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/joho/godotenv"

	"transaction-simulator/config"
	"transaction-simulator/controller"
	"transaction-simulator/utils"
)

func main() {
	// ── 1. Load .env ────────────────────────────────────────────────────────
	// Silently ignored when running in a container where vars are injected.
	if err := godotenv.Load(); err != nil {
		slog.Info(".env not found — using environment variables directly")
	}

	// ── 2. Configuration ────────────────────────────────────────────────────
	cfg, err := config.Load()
	if err != nil {
		slog.Error("Failed to load configuration", "error", err)
		os.Exit(1)
	}

	// ── 3. Logger ───────────────────────────────────────────────────────────
	logger := utils.NewLogger(cfg.Log.Level, cfg.Log.Format)
	slog.SetDefault(logger)

	slog.Info("Configuration loaded",
		"port", cfg.Server.Port,
		"log_level", cfg.Log.Level,
		"log_format", cfg.Log.Format,
		"transaction_api_url", cfg.Target.TransactionAPIURL,
	)

	// ── 4. Router ───────────────────────────────────────────────────────────
	r := buildRouter()

	// ── 5. HTTP Server ──────────────────────────────────────────────────────
	addr := fmt.Sprintf(":%s", cfg.Server.Port)
	srv := &http.Server{
		Addr:         addr,
		Handler:      r,
		ReadTimeout:  cfg.Server.ReadTimeout,
		WriteTimeout: cfg.Server.WriteTimeout,
		IdleTimeout:  cfg.Server.IdleTimeout,
	}

	// Start server in a goroutine so the main goroutine can wait for signals.
	go func() {
		slog.Info("Transaction Simulator started", "addr", addr)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			slog.Error("Server encountered a fatal error", "error", err)
			os.Exit(1)
		}
	}()

	// ── 6. Graceful Shutdown ────────────────────────────────────────────────
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	sig := <-quit

	slog.Info("Shutdown signal received", "signal", sig.String())

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	if err := srv.Shutdown(ctx); err != nil {
		slog.Error("Graceful shutdown failed", "error", err)
		os.Exit(1)
	}

	slog.Info("Server stopped cleanly")
}

// buildRouter wires all middleware and route handlers onto a chi.Router.
func buildRouter() chi.Router {
	r := chi.NewRouter()

	// ── Middleware stack ────────────────────────────────────────────────────
	r.Use(middleware.RequestID)   // adds X-Request-Id header
	r.Use(middleware.RealIP)      // reads X-Forwarded-For / X-Real-IP
	r.Use(middleware.Logger)      // structured request logs
	r.Use(middleware.Recoverer)   // recovers from panics and returns 500

	// ── Routes ──────────────────────────────────────────────────────────────
	healthCtrl := controller.NewHealthController()
	r.Get("/health", healthCtrl.Check)

	// TODO: register simulation endpoints in future milestones.

	return r
}

