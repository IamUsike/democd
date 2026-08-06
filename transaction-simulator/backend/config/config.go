package config

import (
	"fmt"
	"net/url"
	"os"
	"strconv"
	"strings"
	"time"
)

// Config holds all application configuration read from environment variables.
type Config struct {
	Server ServerConfig
	Log    LogConfig
	Target TargetConfig
}

// ServerConfig holds HTTP server tuning parameters.
type ServerConfig struct {
	Port         string
	ReadTimeout  time.Duration
	WriteTimeout time.Duration
	IdleTimeout  time.Duration
}

// LogConfig controls the structured logger.
type LogConfig struct {
	// Level is one of: debug | info | warn | error
	Level string
	// Format is one of: json | text
	Format string
}

// TargetConfig holds connection details for the external Transaction Monitoring API.
type TargetConfig struct {
	// TransactionAPIURL is the full URL of the ingest endpoint on the
	// Spring Boot Transaction Monitoring backend.
	// Loaded from TRANSACTION_API_URL — required, no default.
	// Example: http://localhost:8081/api/v1/transactions
	TransactionAPIURL string

	// Timeout is the per-request HTTP timeout for outbound calls.
	// Loaded from TRANSACTION_API_TIMEOUT — defaults to 30s.
	Timeout time.Duration
}

// Load reads configuration from environment variables, applies defaults for
// optional variables, and enforces that all required variables are present
// and valid.
//
// Required variables — Load returns an error when any of these are absent:
//
//	TRANSACTION_API_URL
//
// Optional variables — defaults are applied when absent:
//
//	SERVER_PORT              (default: 8090)
//	SERVER_READ_TIMEOUT      (default: 10s)
//	SERVER_WRITE_TIMEOUT     (default: 10s)
//	SERVER_IDLE_TIMEOUT      (default: 60s)
//	LOG_LEVEL                (default: info)
//	LOG_FORMAT               (default: json)
//	TRANSACTION_API_TIMEOUT  (default: 30s)
func Load() (*Config, error) {
	// ── Required variables ───────────────────────────────────────────────────
	// Collect ALL missing required vars before returning so the operator sees
	// every problem in a single error message.
	required, err := requireEnvAll(
		"TRANSACTION_API_URL",
	)
	if err != nil {
		return nil, err
	}

	// ── Build config ─────────────────────────────────────────────────────────
	cfg := &Config{
		Server: ServerConfig{
			Port:         getEnv("SERVER_PORT", "8090"),
			ReadTimeout:  getDuration("SERVER_READ_TIMEOUT", 10*time.Second),
			WriteTimeout: getDuration("SERVER_WRITE_TIMEOUT", 10*time.Second),
			IdleTimeout:  getDuration("SERVER_IDLE_TIMEOUT", 60*time.Second),
		},
		Log: LogConfig{
			Level:  getEnv("LOG_LEVEL", "info"),
			Format: getEnv("LOG_FORMAT", "json"),
		},
		Target: TargetConfig{
			TransactionAPIURL: required["TRANSACTION_API_URL"],
			Timeout:           getDuration("TRANSACTION_API_TIMEOUT", 30*time.Second),
		},
	}

	// ── Validate ─────────────────────────────────────────────────────────────
	if err := validate(cfg); err != nil {
		return nil, err
	}

	return cfg, nil
}

// validate checks structural constraints on the assembled configuration.
func validate(cfg *Config) error {
	// Server port must be a valid TCP port number.
	port, err := strconv.Atoi(cfg.Server.Port)
	if err != nil || port < 1 || port > 65535 {
		return fmt.Errorf("config: invalid SERVER_PORT %q — must be an integer 1–65535", cfg.Server.Port)
	}

	// Transaction API URL must be a well-formed HTTP or HTTPS URL.
	if err := validateURL("TRANSACTION_API_URL", cfg.Target.TransactionAPIURL); err != nil {
		return err
	}

	return nil
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

// requireEnvAll checks that every listed environment variable is set and
// non-empty. It reports ALL missing variables in a single error so the
// operator can fix them in one go.
func requireEnvAll(keys ...string) (map[string]string, error) {
	values := make(map[string]string, len(keys))
	var missing []string

	for _, key := range keys {
		v := os.Getenv(key)
		if strings.TrimSpace(v) == "" {
			missing = append(missing, key)
		} else {
			values[key] = v
		}
	}

	if len(missing) > 0 {
		return nil, fmt.Errorf(
			"config: required environment variable(s) not set: %s",
			strings.Join(missing, ", "),
		)
	}

	return values, nil
}

// validateURL checks that value is a well-formed absolute HTTP or HTTPS URL.
func validateURL(varName, value string) error {
	u, err := url.ParseRequestURI(value)
	if err != nil {
		return fmt.Errorf("config: %s %q is not a valid URL: %w", varName, value, err)
	}
	if u.Scheme != "http" && u.Scheme != "https" {
		return fmt.Errorf(
			"config: %s %q must use http or https (got %q)",
			varName, value, u.Scheme,
		)
	}
	if u.Host == "" {
		return fmt.Errorf("config: %s %q is missing a host", varName, value)
	}
	return nil
}

// getEnv returns the value of key, or defaultVal when the variable is absent
// or blank.
func getEnv(key, defaultVal string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return defaultVal
}

// getDuration parses key as a Go duration string (e.g. "10s", "1m30s").
// Falls back to defaultVal when the variable is absent or cannot be parsed.
func getDuration(key string, defaultVal time.Duration) time.Duration {
	v := os.Getenv(key)
	if v == "" {
		return defaultVal
	}
	d, err := time.ParseDuration(v)
	if err != nil {
		return defaultVal
	}
	return d
}
