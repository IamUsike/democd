.PHONY: run build tidy lint clean test integration-test test-short help

## run: start the simulator in development mode (reads .env)
run:
	go run ./cmd

## build: compile a static binary to bin/
build:
	CGO_ENABLED=0 go build -ldflags="-s -w" -o bin/transaction-simulator ./cmd

## tidy: sync go.mod / go.sum and remove unused imports
tidy:
	go mod tidy

## test: run all unit tests + integration tests
test:
	go test ./... -v -race -timeout 60s

## integration-test: run only integration tests
integration-test:
	go test ./test -v -race -timeout 60s

## test-short: run tests, skipping long-running integration tests
test-short:
	go test ./... -short -v -timeout 30s

## lint: run golangci-lint (must be installed separately)
lint:
	golangci-lint run ./...

## clean: remove build artefacts
clean:
	rm -rf bin/

## help: list available targets
help:
	@grep -E '^## ' Makefile | sed 's/## /  /'

