package config_test

import (
	"strings"
	"testing"

	"transaction-simulator/config"
)

// setEnv sets multiple environment variables for the duration of a test and
// restores them (or unsets them) via t.Cleanup.
func setEnv(t *testing.T, pairs map[string]string) {
	t.Helper()
	for k, v := range pairs {
		t.Setenv(k, v)
	}
}

// minimalEnv returns the smallest valid set of environment variables required
// to make config.Load() succeed.
func minimalEnv() map[string]string {
	return map[string]string{
		"TRANSACTION_API_URL": "http://localhost:8081/api/v1/transactions",
	}
}

// ─── Required variable checks ─────────────────────────────────────────────────

func TestLoad_MissingTransactionAPIURL_ReturnsError(t *testing.T) {
	// Do NOT set TRANSACTION_API_URL — all other optional vars have defaults.
	_, err := config.Load()
	if err == nil {
		t.Fatal("expected error when TRANSACTION_API_URL is not set, got nil")
	}
	if !strings.Contains(err.Error(), "TRANSACTION_API_URL") {
		t.Errorf("error should mention TRANSACTION_API_URL, got: %v", err)
	}
}

func TestLoad_EmptyTransactionAPIURL_ReturnsError(t *testing.T) {
	t.Setenv("TRANSACTION_API_URL", "   ") // whitespace only — treated as empty
	_, err := config.Load()
	if err == nil {
		t.Fatal("expected error for blank TRANSACTION_API_URL, got nil")
	}
	if !strings.Contains(err.Error(), "TRANSACTION_API_URL") {
		t.Errorf("error should mention TRANSACTION_API_URL, got: %v", err)
	}
}

// ─── URL validation ───────────────────────────────────────────────────────────

func TestLoad_InvalidURL_NotAbsolute_ReturnsError(t *testing.T) {
	setEnv(t, minimalEnv())
	t.Setenv("TRANSACTION_API_URL", "/api/v1/transactions") // relative — no host
	_, err := config.Load()
	if err == nil {
		t.Fatal("expected error for relative URL, got nil")
	}
}

func TestLoad_InvalidURL_UnsupportedScheme_ReturnsError(t *testing.T) {
	setEnv(t, minimalEnv())
	t.Setenv("TRANSACTION_API_URL", "ftp://example.com/api/v1/transactions")
	_, err := config.Load()
	if err == nil {
		t.Fatal("expected error for ftp:// scheme, got nil")
	}
	if !strings.Contains(err.Error(), "http or https") {
		t.Errorf("error should mention http or https, got: %v", err)
	}
}

func TestLoad_ValidHTTPS_URL(t *testing.T) {
	setEnv(t, minimalEnv())
	t.Setenv("TRANSACTION_API_URL", "https://api.bank.example.com/api/v1/transactions")
	cfg, err := config.Load()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if cfg.Target.TransactionAPIURL != "https://api.bank.example.com/api/v1/transactions" {
		t.Errorf("unexpected TransactionAPIURL: %s", cfg.Target.TransactionAPIURL)
	}
}

// ─── Port validation ──────────────────────────────────────────────────────────

func TestLoad_InvalidPort_Zero_ReturnsError(t *testing.T) {
	setEnv(t, minimalEnv())
	t.Setenv("SERVER_PORT", "0")
	_, err := config.Load()
	if err == nil {
		t.Fatal("expected error for SERVER_PORT=0, got nil")
	}
}

func TestLoad_InvalidPort_NonNumeric_ReturnsError(t *testing.T) {
	setEnv(t, minimalEnv())
	t.Setenv("SERVER_PORT", "abc")
	_, err := config.Load()
	if err == nil {
		t.Fatal("expected error for non-numeric SERVER_PORT, got nil")
	}
}

func TestLoad_InvalidPort_OutOfRange_ReturnsError(t *testing.T) {
	setEnv(t, minimalEnv())
	t.Setenv("SERVER_PORT", "99999")
	_, err := config.Load()
	if err == nil {
		t.Fatal("expected error for SERVER_PORT=99999, got nil")
	}
}

// ─── Happy path ───────────────────────────────────────────────────────────────

func TestLoad_DefaultsApplied(t *testing.T) {
	setEnv(t, minimalEnv())

	cfg, err := config.Load()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	assertEqual(t, "Server.Port", "8090", cfg.Server.Port)
	assertEqual(t, "Log.Level", "info", cfg.Log.Level)
	assertEqual(t, "Log.Format", "json", cfg.Log.Format)

	if cfg.Server.ReadTimeout.Seconds() != 10 {
		t.Errorf("ReadTimeout: want 10s, got %v", cfg.Server.ReadTimeout)
	}
	if cfg.Server.WriteTimeout.Seconds() != 10 {
		t.Errorf("WriteTimeout: want 10s, got %v", cfg.Server.WriteTimeout)
	}
	if cfg.Server.IdleTimeout.Seconds() != 60 {
		t.Errorf("IdleTimeout: want 60s, got %v", cfg.Server.IdleTimeout)
	}
	if cfg.Target.Timeout.Seconds() != 30 {
		t.Errorf("Target.Timeout: want 30s, got %v", cfg.Target.Timeout)
	}
}

func TestLoad_OverridesApplied(t *testing.T) {
	setEnv(t, map[string]string{
		"TRANSACTION_API_URL":     "http://localhost:8081/api/v1/transactions",
		"SERVER_PORT":             "9090",
		"LOG_LEVEL":               "debug",
		"LOG_FORMAT":              "text",
		"SERVER_READ_TIMEOUT":     "5s",
		"SERVER_WRITE_TIMEOUT":    "5s",
		"SERVER_IDLE_TIMEOUT":     "120s",
		"TRANSACTION_API_TIMEOUT": "15s",
	})

	cfg, err := config.Load()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	assertEqual(t, "Server.Port", "9090", cfg.Server.Port)
	assertEqual(t, "Log.Level", "debug", cfg.Log.Level)
	assertEqual(t, "Log.Format", "text", cfg.Log.Format)

	if cfg.Server.ReadTimeout.Seconds() != 5 {
		t.Errorf("ReadTimeout: want 5s, got %v", cfg.Server.ReadTimeout)
	}
	if cfg.Target.Timeout.Seconds() != 15 {
		t.Errorf("Target.Timeout: want 15s, got %v", cfg.Target.Timeout)
	}
	assertEqual(t, "Target.TransactionAPIURL",
		"http://localhost:8081/api/v1/transactions",
		cfg.Target.TransactionAPIURL)
}

func TestLoad_TransactionAPIURL_StoredExactly(t *testing.T) {
	url := "http://localhost:8081/api/v1/transactions"
	setEnv(t, map[string]string{"TRANSACTION_API_URL": url})

	cfg, err := config.Load()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if cfg.Target.TransactionAPIURL != url {
		t.Errorf("want %q, got %q", url, cfg.Target.TransactionAPIURL)
	}
}

// ─── helper ───────────────────────────────────────────────────────────────────

func assertEqual(t *testing.T, field, want, got string) {
	t.Helper()
	if want != got {
		t.Errorf("%s: want %q, got %q", field, want, got)
	}
}

