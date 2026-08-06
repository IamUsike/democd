# Multi-stage build for Transaction Simulator backend
# Stage 1: Build
# Stage 2: Production runtime

# ─────────────────────────────────────────────────────────────────────────────
# Stage 1: Build static Go binary
# ─────────────────────────────────────────────────────────────────────────────
FROM golang:1.22.0-alpine AS builder

# Install build dependencies (minimal)
RUN apk add --no-cache ca-certificates git

# Set working directory
WORKDIR /build

# Copy go mod and sum files
COPY go.mod go.sum ./

# Download dependencies
RUN go mod download

# Copy entire source tree
COPY . .

# Build static binary
# CGO_ENABLED=0: no C dependencies, fully static
# -ldflags="-s -w": strip debug symbols and DWARF tables for smaller size
# -o /build/bin/transaction-simulator: output path
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 \
    go build \
    -ldflags="-s -w -X main.version=${VERSION:-dev}" \
    -o /build/bin/transaction-simulator \
    ./cmd

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2: Production runtime image
# ─────────────────────────────────────────────────────────────────────────────
FROM alpine:3.20

# Install minimal runtime dependencies (ca-certificates for HTTPS)
RUN apk add --no-cache ca-certificates tzdata

# Create non-root user for security
RUN addgroup -g 1001 simulator && \
    adduser -u 1001 -G simulator -s /sbin/nologin -D simulator

# Set working directory and copy binary from builder
WORKDIR /app
COPY --from=builder /build/bin/transaction-simulator .

# Change ownership to non-root user
RUN chown simulator:simulator /app/transaction-simulator

# Switch to non-root user
USER simulator

# Expose backend API port (can be overridden via SERVER_PORT env var)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${SERVER_PORT:-8080}/health || exit 1

# Run the application
# Environment variables can be injected via docker run -e or docker-compose
CMD ["./transaction-simulator"]

