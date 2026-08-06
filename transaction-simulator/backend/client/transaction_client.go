package client

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log/slog"
	"net"
	"net/http"
	"net/url"
	"strings"
	"time"

	"transaction-simulator/config"
	"transaction-simulator/model"
)

const maxResponseBodyLogLength = 512

// TransactionClient sends generated transactions to the Spring Boot
// Transaction Monitoring backend.
type TransactionClient struct {
	baseURL    string
	httpClient *http.Client
	logger     *slog.Logger
	timeout    time.Duration
}

// apiEnvelope is the minimal response contract expected from the monitoring API.
// The Spring Boot backend wraps responses in an ApiResponse envelope.
type apiEnvelope struct {
	Success *bool  `json:"success"`
	Message string `json:"message"`
}

// NewTransactionClient builds a production-ready HTTP client for sending
// transactions to the monitoring backend.
//
// Dependencies:
//   - target.TransactionAPIURL: required full POST endpoint URL
//   - httpClient: optional; when nil a default client is created
//   - logger: optional; when nil slog.Default() is used
func NewTransactionClient(
	target config.TargetConfig,
	httpClient *http.Client,
	logger *slog.Logger,
) (*TransactionClient, error) {
	if err := validateTransactionAPIURL(target.TransactionAPIURL); err != nil {
		return nil, err
	}

	if logger == nil {
		logger = slog.Default()
	}

	timeout := target.Timeout
	if timeout <= 0 {
		timeout = 30 * time.Second
	}

	resolvedHTTPClient := resolveHTTPClient(httpClient, timeout)

	return &TransactionClient{
		baseURL:    target.TransactionAPIURL,
		httpClient: resolvedHTTPClient,
		logger:     logger,
		timeout:    timeout,
	}, nil
}

// SendTransaction marshals tx to JSON and POSTs it to the configured
// TRANSACTION_API_URL endpoint.
//
// Status handling:
//   - 2xx: success, but the JSON ApiResponse envelope must still be valid
//   - 4xx: returns a validation/client error
//   - 5xx: returns a server error
func (c *TransactionClient) SendTransaction(tx model.Transaction) error {
	payload, err := json.Marshal(tx)
	if err != nil {
		return fmt.Errorf("send transaction: marshal request JSON: %w", err)
	}

	ctx := context.Background()
	var cancel context.CancelFunc
	if c.timeout > 0 {
		ctx, cancel = context.WithTimeout(ctx, c.timeout)
		defer cancel()
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.baseURL, bytes.NewReader(payload))
	if err != nil {
		return fmt.Errorf("send transaction: build request: %w", err)
	}
	req.Header.Set("Content-Type", "application/json")

	startedAt := time.Now()
	c.logger.Debug("sending transaction",
		"url", c.baseURL,
		"accountId", tx.AccountID,
		"sourceType", tx.SourceType,
		"amount", tx.Amount,
	)

	resp, err := c.httpClient.Do(req)
	if err != nil {
		if isTimeoutError(err) {
			c.logger.Error("transaction request timed out",
				"url", c.baseURL,
				"timeout", c.timeout.String(),
				"error", err,
			)
			return fmt.Errorf("send transaction: request timed out after %s: %w", c.timeout, err)
		}

		c.logger.Error("transaction request failed",
			"url", c.baseURL,
			"error", err,
		)
		return fmt.Errorf("send transaction: request failed: %w", err)
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(io.LimitReader(resp.Body, maxResponseBodyLogLength))
	if err != nil {
		return fmt.Errorf("send transaction: read response body: %w", err)
	}

	latency := time.Since(startedAt)
	message := extractMessage(body)

	switch {
	case resp.StatusCode >= 200 && resp.StatusCode < 300:
		if err := validateSuccessResponse(body); err != nil {
			c.logger.Error("transaction API returned invalid success payload",
				"status_code", resp.StatusCode,
				"latency_ms", latency.Milliseconds(),
				"error", err,
			)
			return err
		}

		c.logger.Info("transaction sent successfully",
			"status_code", resp.StatusCode,
			"latency_ms", latency.Milliseconds(),
			"accountId", tx.AccountID,
		)
		return nil

	case resp.StatusCode >= 400 && resp.StatusCode < 500:
		c.logger.Warn("transaction API rejected request",
			"status_code", resp.StatusCode,
			"latency_ms", latency.Milliseconds(),
			"message", message,
		)
		return fmt.Errorf("send transaction: client error %d: %s", resp.StatusCode, message)

	case resp.StatusCode >= 500:
		c.logger.Error("transaction API server error",
			"status_code", resp.StatusCode,
			"latency_ms", latency.Milliseconds(),
			"message", message,
		)
		return fmt.Errorf("send transaction: server error %d: %s", resp.StatusCode, message)

	default:
		c.logger.Error("transaction API unexpected response",
			"status_code", resp.StatusCode,
			"latency_ms", latency.Milliseconds(),
			"message", message,
		)
		return fmt.Errorf("send transaction: unexpected response status %d: %s", resp.StatusCode, message)
	}
}

func resolveHTTPClient(httpClient *http.Client, timeout time.Duration) *http.Client {
	if httpClient == nil {
		return &http.Client{Timeout: timeout}
	}

	cloned := *httpClient
	if cloned.Timeout <= 0 {
		cloned.Timeout = timeout
	}
	return &cloned
}

func validateTransactionAPIURL(rawURL string) error {
	parsed, err := url.ParseRequestURI(rawURL)
	if err != nil {
		return fmt.Errorf("transaction client: invalid TRANSACTION_API_URL %q: %w", rawURL, err)
	}
	if parsed.Scheme != "http" && parsed.Scheme != "https" {
		return fmt.Errorf(
			"transaction client: TRANSACTION_API_URL %q must use http or https (got %q)",
			rawURL, parsed.Scheme,
		)
	}
	if parsed.Host == "" {
		return fmt.Errorf("transaction client: TRANSACTION_API_URL %q is missing a host", rawURL)
	}
	return nil
}

func validateSuccessResponse(body []byte) error {
	trimmed := strings.TrimSpace(string(body))
	if trimmed == "" {
		return nil
	}
	if !json.Valid(body) {
		return fmt.Errorf("send transaction: invalid API response: expected JSON envelope, got %q", truncate(trimmed))
	}

	var envelope apiEnvelope
	if err := json.Unmarshal(body, &envelope); err != nil {
		return fmt.Errorf("send transaction: invalid API response: %w", err)
	}
	if envelope.Success == nil {
		return fmt.Errorf("send transaction: invalid API response: missing success field")
	}
	if !*envelope.Success {
		message := strings.TrimSpace(envelope.Message)
		if message == "" {
			message = "API reported unsuccessful response"
		}
		return fmt.Errorf("send transaction: API reported failure: %s", message)
	}
	return nil
}

func extractMessage(body []byte) string {
	trimmed := strings.TrimSpace(string(body))
	if trimmed == "" {
		return http.StatusText(http.StatusBadRequest)
	}

	var envelope apiEnvelope
	if json.Valid(body) && json.Unmarshal(body, &envelope) == nil && strings.TrimSpace(envelope.Message) != "" {
		return envelope.Message
	}

	return truncate(trimmed)
}

func truncate(value string) string {
	if len(value) <= maxResponseBodyLogLength {
		return value
	}
	return value[:maxResponseBodyLogLength] + "..."
}

func isTimeoutError(err error) bool {
	if errors.Is(err, context.DeadlineExceeded) {
		return true
	}

	var netErr net.Error
	return errors.As(err, &netErr) && netErr.Timeout()
}

