.PHONY: run build tidy lint clean test help

## run: start the simulator in development mode (reads .env)
run:
	go run ./cmd

## build: compile a static binary to bin/
build:
	CGO_ENABLED=0 go build -ldflags="-s -w" -o bin/transaction-simulator ./cmd

## tidy: sync go.mod / go.sum and remove unused imports
tidy:
	go mod tidy

## test: run all unit tests
test:
	go test ./... -v -race -timeout 60s

## lint: run golangci-lint (must be installed separately)
lint:
	golangci-lint run ./...

## clean: remove build artefacts
clean:
	rm -rf bin/

## help: list available targets
help:
	@grep -E '^## ' Makefile | sed 's/## /  /'

