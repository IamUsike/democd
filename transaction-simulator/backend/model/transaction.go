package model

import (
	"fmt"
	"strings"
	"time"
)

// ─────────────────────────────────────────────────────────────────────────────
// Enums
// ─────────────────────────────────────────────────────────────────────────────

// SourceType identifies whether a transaction originated from a bank or a merchant.
// Matches the source_type VARCHAR(20) column and the allowed values defined in
// the transactions table constraint.
type SourceType string

const (
	SourceTypeBank     SourceType = "BANK"
	SourceTypeMerchant SourceType = "MERCHANT"
)

// TransactionType describes the direction / nature of a financial movement.
// Matches the type VARCHAR(30) column in the transactions table.
type TransactionType string

const (
	TransactionTypeDebit    TransactionType = "DEBIT"
	TransactionTypeCredit   TransactionType = "CREDIT"
	TransactionTypeTransfer TransactionType = "TRANSFER"
)

// TransactionStatus represents the processing outcome of a transaction.
// Matches the status VARCHAR(20) column in the transactions table.
type TransactionStatus string

const (
	TransactionStatusCompleted TransactionStatus = "COMPLETED"
	TransactionStatusFailed    TransactionStatus = "FAILED"
)

// ─────────────────────────────────────────────────────────────────────────────
// LocalDateTime — custom time type
// ─────────────────────────────────────────────────────────────────────────────

// localDateTimeLayout is the wire format used by the Spring Boot backend.
// It intentionally omits the timezone offset, matching Java's LocalDateTime.
const localDateTimeLayout = "2006-01-02T15:04:05"

// LocalDateTime wraps time.Time and marshals / unmarshals as the Java
// LocalDateTime wire format "yyyy-MM-dd'T'HH:mm:ss" (no timezone offset),
// matching the Spring Boot Transaction Monitoring API contract exactly.
//
// Example: "2026-08-06T10:30:00"
type LocalDateTime struct {
	time.Time
}

// MarshalJSON encodes the timestamp as "yyyy-MM-ddTHH:mm:ss".
func (t LocalDateTime) MarshalJSON() ([]byte, error) {
	return []byte(`"` + t.Time.Format(localDateTimeLayout) + `"`), nil
}

// UnmarshalJSON decodes a "yyyy-MM-ddTHH:mm:ss" JSON string.
// Returns an error for any format that does not match the layout.
func (t *LocalDateTime) UnmarshalJSON(data []byte) error {
	s := strings.Trim(string(data), `"`)
	if s == "null" {
		return nil
	}
	parsed, err := time.Parse(localDateTimeLayout, s)
	if err != nil {
		return fmt.Errorf("LocalDateTime: cannot parse %q (expected format %q): %w",
			s, localDateTimeLayout, err)
	}
	t.Time = parsed
	return nil
}

// NowLocal returns the current local time wrapped in LocalDateTime.
func NowLocal() LocalDateTime {
	return LocalDateTime{Time: time.Now()}
}

// ─────────────────────────────────────────────────────────────────────────────
// Transaction
// ─────────────────────────────────────────────────────────────────────────────

// Transaction is the payload posted to POST /api/v1/transactions.
//
// Field names, JSON tags, and data types mirror the Spring Boot backend
// transactions table schema exactly:
//
//	VARCHAR  → string  (or *string when the column is NULL-able)
//	DECIMAL  → float64 (or *float64 when the column is NULL-able)
//	DATETIME → LocalDateTime
//
// Required columns (NOT NULL in the DB):
//
//	source_type, source_id, source_name, account_id, payee_id,
//	amount, currency, type, timestamp, status
//
// Optional / NULL-able columns in the DB (pointer types, omitted when nil):
//
//	payee_name, location, latitude, longitude, description
type Transaction struct {
	// ── Source ───────────────────────────────────────────────────────────────
	// Identifies the originating bank or merchant feed.
	// source_type VARCHAR(20) NOT NULL — "BANK" | "MERCHANT"
	SourceType SourceType `json:"sourceType"`

	// source_id VARCHAR(64) NOT NULL — stable opaque code, e.g. "HSBC-UK"
	SourceID string `json:"sourceId"`

	// source_name VARCHAR(128) NOT NULL — display name, e.g. "HSBC United Kingdom"
	SourceName string `json:"sourceName"`

	// ── Account ──────────────────────────────────────────────────────────────
	// The monitored party that sent the transaction.
	// account_id VARCHAR(64) NOT NULL — opaque ID, not a foreign key
	AccountID string `json:"accountId"`

	// ── Payee ────────────────────────────────────────────────────────────────
	// The counterparty receiving the transaction.
	// payee_id VARCHAR(64) NOT NULL — opaque ID
	PayeeID string `json:"payeeId"`

	// payee_name VARCHAR(128) NULL — display name, omitted when not provided
	PayeeName *string `json:"payeeName,omitempty"`

	// ── Financial ────────────────────────────────────────────────────────────
	// amount DECIMAL(15,2) NOT NULL — must be greater than zero
	Amount float64 `json:"amount"`

	// currency VARCHAR(3) NOT NULL — ISO 4217 code, e.g. "USD", "INR"
	Currency string `json:"currency"`

	// type VARCHAR(30) NOT NULL — DEBIT | CREDIT | TRANSFER
	Type TransactionType `json:"type"`

	// ── Temporal ─────────────────────────────────────────────────────────────
	// timestamp DATETIME NOT NULL — when the transaction occurred.
	// Serialised as "yyyy-MM-dd'T'HH:mm:ss" (Java LocalDateTime format).
	Timestamp LocalDateTime `json:"timestamp"`

	// ── Geographic (optional) ────────────────────────────────────────────────
	// location VARCHAR(255) NULL — free-text label, e.g. "London"
	Location *string `json:"location,omitempty"`

	// latitude DECIMAL(10,7) NULL — optional geo coordinate
	Latitude *float64 `json:"latitude,omitempty"`

	// longitude DECIMAL(10,7) NULL — optional geo coordinate
	Longitude *float64 `json:"longitude,omitempty"`

	// ── Metadata ─────────────────────────────────────────────────────────────
	// description VARCHAR(255) NULL — optional free-text note
	Description *string `json:"description,omitempty"`

	// status VARCHAR(20) NOT NULL — COMPLETED | FAILED
	Status TransactionStatus `json:"status"`
}

