// Package client contains typed HTTP clients for external services.
//
// The primary client posts generated transactions to the Transaction
// Monitoring API. All outbound calls are context-aware, time-limited,
// and return structured errors that the service layer can inspect.
package client

