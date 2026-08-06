package utils

import (
	"log/slog"
	"os"
	"strings"
)

// NewLogger builds a *slog.Logger configured for the given level and format.
//
// level  — one of: debug | info | warn | error (case-insensitive; defaults to info)
// format — one of: json | text  (case-insensitive; defaults to json)
//
// In debug mode AddSource is enabled so log lines include file:line.
func NewLogger(level, format string) *slog.Logger {
	lvl := parseLevel(level)

	opts := &slog.HandlerOptions{
		Level:     lvl,
		AddSource: lvl == slog.LevelDebug,
	}

	var handler slog.Handler
	if strings.EqualFold(format, "text") {
		handler = slog.NewTextHandler(os.Stdout, opts)
	} else {
		handler = slog.NewJSONHandler(os.Stdout, opts)
	}

	return slog.New(handler)
}

// parseLevel converts a level string to the corresponding slog.Level.
// Any unrecognised value falls back to slog.LevelInfo.
func parseLevel(level string) slog.Level {
	switch strings.ToLower(strings.TrimSpace(level)) {
	case "debug":
		return slog.LevelDebug
	case "warn", "warning":
		return slog.LevelWarn
	case "error":
		return slog.LevelError
	default:
		return slog.LevelInfo
	}
}

