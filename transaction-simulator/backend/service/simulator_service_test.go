package service

import (
	"errors"
	"io"
	"log/slog"
	"sync"
	"sync/atomic"
	"testing"
	"time"

	"transaction-simulator/generator"
	"transaction-simulator/model"
)

type mockGenerator struct {
	count atomic.Uint64
}

func (m *mockGenerator) Generate(mode generator.SimulationMode) model.Transaction {
	current := m.count.Add(1)
	payeeName := "Amazon"
	location := "London"
	latitude := 51.5074
	longitude := -0.1278
	description := "Generated transaction"

	accountID := "ACC10001"
	if mode == generator.ModeFraud {
		accountID = "ACC-FRAUD-001"
	}

	return model.Transaction{
		SourceType:  model.SourceTypeBank,
		SourceID:    "HSBC-UK",
		SourceName:  "HSBC United Kingdom",
		AccountID:   accountID,
		PayeeID:     "PAYEE10001",
		PayeeName:   &payeeName,
		Amount:      float64(current),
		Currency:    "USD",
		Type:        model.TransactionTypeTransfer,
		Timestamp:   model.NowLocal(),
		Location:    &location,
		Latitude:    &latitude,
		Longitude:   &longitude,
		Description: &description,
		Status:      model.TransactionStatusCompleted,
	}
}

type mockSender struct {
	mu           sync.Mutex
	sent         []model.Transaction
	callCount    atomic.Uint64
	failEvery    uint64
	blockUntilCh <-chan struct{}
}

func (m *mockSender) SendTransaction(tx model.Transaction) error {
	if m.blockUntilCh != nil {
		<-m.blockUntilCh
	}

	callNumber := m.callCount.Add(1)

	m.mu.Lock()
	m.sent = append(m.sent, tx)
	m.mu.Unlock()

	if m.failEvery > 0 && callNumber%m.failEvery == 0 {
		return errors.New("simulated send failure")
	}

	return nil
}

func (m *mockSender) sentCount() uint64 {
	return m.callCount.Load()
}

func discardLogger() *slog.Logger {
	return slog.New(slog.NewTextHandler(io.Discard, nil))
}

func newTestService(t *testing.T, gen TransactionGenerator, sender TransactionSender) *SimulatorService {
	t.Helper()

	svc, err := newSimulatorService(gen, sender, discardLogger(), runtimeConfig{
		generatorWorkers: 2,
		senderWorkers:    4,
		channelBuffer:    64,
		rateIntervalBase: 100 * time.Millisecond,
		durationUnit:     100 * time.Millisecond,
		metricsInterval:  100 * time.Millisecond,
	})
	if err != nil {
		t.Fatalf("newSimulatorService returned error: %v", err)
	}
	return svc
}

func TestNewSimulatorService_RequiresDependencies(t *testing.T) {
	sender := &mockSender{}
	gen := &mockGenerator{}

	if _, err := NewSimulatorService(nil, sender, discardLogger()); err == nil {
		t.Fatal("expected error when generator is nil")
	}
	if _, err := NewSimulatorService(gen, nil, discardLogger()); err == nil {
		t.Fatal("expected error when sender is nil")
	}
}

func TestStartSimulation_ValidatesRequest(t *testing.T) {
	svc := newTestService(t, &mockGenerator{}, &mockSender{})

	cases := []SimulationRequest{
		{TPS: 0, Duration: 1, Mode: generator.ModeNormal},
		{TPS: 100, Duration: 0, Mode: generator.ModeNormal},
		{TPS: 100, Duration: 1, Mode: generator.SimulationMode("UNKNOWN")},
	}

	for _, tc := range cases {
		if err := svc.Start(tc); err == nil {
			t.Fatalf("expected validation error for request %+v", tc)
		}
	}
}

func TestStartSimulation_SetsRunningAndCompletes(t *testing.T) {
	svc := newTestService(t, &mockGenerator{}, &mockSender{})

	err := svc.Start(SimulationRequest{TPS: 10, Duration: 2, Mode: generator.ModeNormal})
	if err != nil {
		t.Fatalf("Start returned error: %v", err)
	}

	if !svc.Metrics().Running {
		t.Fatal("expected simulator to be running immediately after Start")
	}

	waitFor(t, time.Second, func() bool {
		return !svc.Metrics().Running
	}, "simulation to finish automatically")
}

func TestStopSimulation_StopsRunningWorkersSafely(t *testing.T) {
	blockCh := make(chan struct{})
	sender := &mockSender{blockUntilCh: blockCh}
	svc := newTestService(t, &mockGenerator{}, sender)

	err := svc.Start(SimulationRequest{TPS: 20, Duration: 10, Mode: generator.ModeNormal})
	if err != nil {
		t.Fatalf("Start returned error: %v", err)
	}

	waitFor(t, time.Second, func() bool {
		return svc.Metrics().TransactionsGenerated > 0
	}, "transactions to start generating")

	close(blockCh)

	if err := svc.Stop(); err != nil {
		t.Fatalf("Stop returned error: %v", err)
	}

	metrics := svc.Metrics()
	if metrics.Running {
		t.Fatal("expected simulator to be stopped after Stop")
	}
	if metrics.CurrentTPS != 0 {
		t.Fatalf("expected currentTPS to reset to 0 after Stop, got %d", metrics.CurrentTPS)
	}
}

func TestMetricsUpdate_TracksSuccessAndFailureCounts(t *testing.T) {
	sender := &mockSender{failEvery: 3}
	svc := newTestService(t, &mockGenerator{}, sender)

	err := svc.Start(SimulationRequest{TPS: 15, Duration: 3, Mode: generator.ModeFraud})
	if err != nil {
		t.Fatalf("Start returned error: %v", err)
	}

	waitFor(t, 2*time.Second, func() bool {
		return !svc.Metrics().Running
	}, "simulation to finish")

	metrics := svc.Metrics()
	if metrics.TransactionsGenerated == 0 {
		t.Fatal("expected generated transactions to be > 0")
	}
	if metrics.SuccessfulTransactions == 0 {
		t.Fatal("expected at least one successful transaction")
	}
	if metrics.FailedTransactions == 0 {
		t.Fatal("expected at least one failed transaction")
	}
	if metrics.TransactionsGenerated != metrics.SuccessfulTransactions+metrics.FailedTransactions {
		t.Fatalf("generated count mismatch: generated=%d successful=%d failed=%d",
			metrics.TransactionsGenerated, metrics.SuccessfulTransactions, metrics.FailedTransactions)
	}
}

func TestWorkerShutdown_NoMoreTransactionsSentAfterStop(t *testing.T) {
	sender := &mockSender{}
	svc := newTestService(t, &mockGenerator{}, sender)

	err := svc.Start(SimulationRequest{TPS: 25, Duration: 10, Mode: generator.ModeNormal})
	if err != nil {
		t.Fatalf("Start returned error: %v", err)
	}

	waitFor(t, time.Second, func() bool {
		return sender.sentCount() > 0
	}, "at least one transaction to be sent")

	if err := svc.Stop(); err != nil {
		t.Fatalf("Stop returned error: %v", err)
	}

	countAfterStop := sender.sentCount()
	time.Sleep(150 * time.Millisecond)
	countLater := sender.sentCount()
	if countLater != countAfterStop {
		t.Fatalf("expected no more sends after Stop, got %d before and %d after", countAfterStop, countLater)
	}
}

func TestTPSCalculation_ApproximatelyMatchesConfiguredRate(t *testing.T) {
	svc := newTestService(t, &mockGenerator{}, &mockSender{})

	err := svc.Start(SimulationRequest{TPS: 10, Duration: 5, Mode: generator.ModeNormal})
	if err != nil {
		t.Fatalf("Start returned error: %v", err)
	}
	defer svc.Stop()

	waitFor(t, time.Second, func() bool {
		return svc.Metrics().CurrentTPS > 0
	}, "currentTPS to update")

	currentTPS := svc.Metrics().CurrentTPS
	if currentTPS < 8 || currentTPS > 12 {
		t.Fatalf("expected currentTPS to be approximately 10, got %d", currentTPS)
	}
}

func TestStartSimulation_RejectsSecondConcurrentRun(t *testing.T) {
	svc := newTestService(t, &mockGenerator{}, &mockSender{})

	err := svc.Start(SimulationRequest{TPS: 10, Duration: 5, Mode: generator.ModeNormal})
	if err != nil {
		t.Fatalf("first Start returned error: %v", err)
	}
	defer svc.Stop()

	err = svc.Start(SimulationRequest{TPS: 10, Duration: 5, Mode: generator.ModeFraud})
	if err == nil {
		t.Fatal("expected second Start call to fail while simulation is running")
	}
}

func TestStopSimulation_WhenNotRunning_IsSafe(t *testing.T) {
	svc := newTestService(t, &mockGenerator{}, &mockSender{})
	if err := svc.Stop(); err != nil {
		t.Fatalf("expected Stop on idle service to succeed, got: %v", err)
	}
}

func waitFor(t *testing.T, timeout time.Duration, condition func() bool, description string) {
	t.Helper()
	deadline := time.Now().Add(timeout)
	for time.Now().Before(deadline) {
		if condition() {
			return
		}
		time.Sleep(10 * time.Millisecond)
	}
	t.Fatalf("timed out waiting for %s", description)
}

