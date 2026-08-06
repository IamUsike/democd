package service

import (
	"errors"
	"fmt"
	"log/slog"
	"math"
	"sync"
	"sync/atomic"
	"time"

	"transaction-simulator/client"
	"transaction-simulator/generator"
	"transaction-simulator/model"
)

const (
	defaultGeneratorWorkers = 4
	defaultSenderWorkers    = 16
	defaultChannelBuffer    = 512
)

// TransactionGenerator is the minimal contract required from the existing
// generator component.
type TransactionGenerator interface {
	Generate(mode generator.SimulationMode) model.Transaction
}

// TransactionSender is the minimal contract required from the existing client
// component.
type TransactionSender interface {
	SendTransaction(tx model.Transaction) error
}

// SimulationRequest describes one simulation run.
type SimulationRequest struct {
	TPS      int                      `json:"tps"`
	Duration int                      `json:"duration"`
	Mode     generator.SimulationMode `json:"mode"`
}

// SimulationMetrics is a thread-safe snapshot of the current or latest run.
type SimulationMetrics struct {
	Running                bool   `json:"running"`
	TransactionsGenerated  uint64 `json:"transactionsGenerated"`
	SuccessfulTransactions uint64 `json:"successfulTransactions"`
	FailedTransactions     uint64 `json:"failedTransactions"`
	CurrentTPS             int    `json:"currentTPS"`
}

type runtimeConfig struct {
	generatorWorkers int
	senderWorkers    int
	channelBuffer    int
	rateIntervalBase time.Duration
	durationUnit     time.Duration
	metricsInterval  time.Duration
}

type simulationRun struct {
	request  SimulationRequest
	cancel   func()
	done     chan struct{}
	stopOnce sync.Once
}

// SimulatorService controls transaction generation, paced sending, and
// thread-safe metrics tracking.
type SimulatorService struct {
	generator TransactionGenerator
	sender    TransactionSender
	logger    *slog.Logger
	cfg       runtimeConfig

	generatorMu sync.Mutex
	stateMu     sync.Mutex
	run         *simulationRun

	generated atomic.Uint64
	successful atomic.Uint64
	failed     atomic.Uint64
	currentTPS atomic.Int64
	running    atomic.Bool
}

// NewSimulatorService creates a simulation engine using the existing generator
// and transaction client components.
func NewSimulatorService(
	transactionGenerator TransactionGenerator,
	transactionClient TransactionSender,
	logger *slog.Logger,
) (*SimulatorService, error) {
	return newSimulatorService(transactionGenerator, transactionClient, logger, runtimeConfig{
		generatorWorkers: defaultGeneratorWorkers,
		senderWorkers:    defaultSenderWorkers,
		channelBuffer:    defaultChannelBuffer,
		rateIntervalBase: time.Second,
		durationUnit:     time.Second,
		metricsInterval:  time.Second,
	})
}

func newSimulatorService(
	transactionGenerator TransactionGenerator,
	transactionClient TransactionSender,
	logger *slog.Logger,
	cfg runtimeConfig,
) (*SimulatorService, error) {
	if transactionGenerator == nil {
		return nil, errors.New("simulator service: transaction generator is required")
	}
	if transactionClient == nil {
		return nil, errors.New("simulator service: transaction client is required")
	}
	if logger == nil {
		logger = slog.Default()
	}
	if cfg.generatorWorkers <= 0 {
		cfg.generatorWorkers = defaultGeneratorWorkers
	}
	if cfg.senderWorkers <= 0 {
		cfg.senderWorkers = defaultSenderWorkers
	}
	if cfg.channelBuffer <= 0 {
		cfg.channelBuffer = defaultChannelBuffer
	}
	if cfg.rateIntervalBase <= 0 {
		cfg.rateIntervalBase = time.Second
	}
	if cfg.durationUnit <= 0 {
		cfg.durationUnit = time.Second
	}
	if cfg.metricsInterval <= 0 {
		cfg.metricsInterval = cfg.rateIntervalBase
	}

	return &SimulatorService{
		generator: transactionGenerator,
		sender:    transactionClient,
		logger:    logger,
		cfg:       cfg,
	}, nil
}

// NewWithComponents is a convenience constructor for the existing concrete
// packages, while still keeping the service mockable in tests.
func NewWithComponents(
	transactionGenerator *generator.Generator,
	transactionClient *client.TransactionClient,
	logger *slog.Logger,
) (*SimulatorService, error) {
	return NewSimulatorService(transactionGenerator, transactionClient, logger)
}

// Start begins a new simulation run.
func (s *SimulatorService) Start(request SimulationRequest) error {
	if err := s.validateRequest(request); err != nil {
		return err
	}

	s.stateMu.Lock()
	if s.run != nil {
		s.stateMu.Unlock()
		return errors.New("simulator service: simulation already running")
	}

	ctx, cancel := newStoppableContext()
	run := &simulationRun{
		request: request,
		cancel:  cancel,
		done:    make(chan struct{}),
	}
	s.run = run
	s.resetMetrics()
	s.running.Store(true)
	s.stateMu.Unlock()

	tokens := make(chan struct{}, s.cfg.generatorWorkers*2)
	transactions := make(chan model.Transaction, s.cfg.channelBuffer)

	var generatorWG sync.WaitGroup
	var senderWG sync.WaitGroup

	go s.runTokenProducer(ctx, request, tokens)

	for workerID := 0; workerID < s.cfg.generatorWorkers; workerID++ {
		generatorWG.Add(1)
		go s.runGeneratorWorker(ctx, request.Mode, workerID, tokens, transactions, &generatorWG)
	}

	go func() {
		generatorWG.Wait()
		close(transactions)
	}()

	for workerID := 0; workerID < s.cfg.senderWorkers; workerID++ {
		senderWG.Add(1)
		go s.runSenderWorker(ctx, workerID, transactions, &senderWG)
	}

	go s.runMetricsUpdater(ctx, run.done)

	go func(currentRun *simulationRun) {
		senderWG.Wait()
		s.finishRun(currentRun)
	}(run)

	s.logger.Info("simulation started",
		"tps", request.TPS,
		"duration", request.Duration,
		"mode", string(request.Mode),
		"generator_workers", s.cfg.generatorWorkers,
		"sender_workers", s.cfg.senderWorkers,
	)

	return nil
}

// Stop requests a graceful shutdown of the current simulation run.
// It is safe to call Stop multiple times.
func (s *SimulatorService) Stop() error {
	s.stateMu.Lock()
	run := s.run
	s.stateMu.Unlock()

	if run == nil {
		return nil
	}

	run.stopOnce.Do(func() {
		s.logger.Info("simulation stop requested",
			"tps", run.request.TPS,
			"duration", run.request.Duration,
			"mode", string(run.request.Mode),
		)
		run.cancel()
	})

	<-run.done
	return nil
}

// Metrics returns a consistent snapshot of the current metrics.
func (s *SimulatorService) Metrics() SimulationMetrics {
	return SimulationMetrics{
		Running:                s.running.Load(),
		TransactionsGenerated:  s.generated.Load(),
		SuccessfulTransactions: s.successful.Load(),
		FailedTransactions:     s.failed.Load(),
		CurrentTPS:             int(s.currentTPS.Load()),
	}
}

func (s *SimulatorService) validateRequest(request SimulationRequest) error {
	if request.TPS <= 0 {
		return errors.New("simulator service: tps must be greater than 0")
	}
	if request.Duration <= 0 {
		return errors.New("simulator service: duration must be greater than 0")
	}
	if request.Mode != generator.ModeNormal && request.Mode != generator.ModeFraud {
		return fmt.Errorf("simulator service: unsupported simulation mode %q", request.Mode)
	}
	return nil
}

func (s *SimulatorService) runTokenProducer(ctx *stoppableContext, request SimulationRequest, tokens chan<- struct{}) {
	defer close(tokens)

	interval := s.tokenInterval(request.TPS)
	ticker := time.NewTicker(interval)
	defer ticker.Stop()

	timer := time.NewTimer(time.Duration(request.Duration) * s.cfg.durationUnit)
	defer timer.Stop()

	for {
		select {
		case <-ctx.Done():
			return
		case <-timer.C:
			return
		case <-ticker.C:
			select {
			case <-ctx.Done():
				return
			case tokens <- struct{}{}:
			}
		}
	}
}

func (s *SimulatorService) runGeneratorWorker(
	ctx *stoppableContext,
	mode generator.SimulationMode,
	workerID int,
	tokens <-chan struct{},
	transactions chan<- model.Transaction,
	wg *sync.WaitGroup,
) {
	defer wg.Done()

	for {
		select {
		case <-ctx.Done():
			return
		case _, ok := <-tokens:
			if !ok {
				return
			}

			s.generatorMu.Lock()
			tx := s.generator.Generate(mode)
			s.generatorMu.Unlock()

			s.generated.Add(1)
			s.logger.Debug("transaction generated",
				"worker", workerID,
				"accountId", tx.AccountID,
				"mode", string(mode),
			)

			select {
			case <-ctx.Done():
				return
			case transactions <- tx:
			}
		}
	}
}

func (s *SimulatorService) runSenderWorker(
	ctx *stoppableContext,
	workerID int,
	transactions <-chan model.Transaction,
	wg *sync.WaitGroup,
) {
	defer wg.Done()

	for {
		select {
		case <-ctx.Done():
			return
		case tx, ok := <-transactions:
			if !ok {
				return
			}

			if err := s.sender.SendTransaction(tx); err != nil {
				s.failed.Add(1)
				s.logger.Warn("transaction send failed",
					"worker", workerID,
					"accountId", tx.AccountID,
					"error", err,
				)
				continue
			}

			s.successful.Add(1)
			s.logger.Debug("transaction sent",
				"worker", workerID,
				"accountId", tx.AccountID,
			)
		}
	}
}

func (s *SimulatorService) runMetricsUpdater(ctx *stoppableContext, done <-chan struct{}) {
	ticker := time.NewTicker(s.cfg.metricsInterval)
	defer ticker.Stop()

	var previousGenerated uint64

	for {
		select {
		case <-ctx.Done():
			return
		case <-done:
			return
		case <-ticker.C:
			currentGenerated := s.generated.Load()
			delta := currentGenerated - previousGenerated
			previousGenerated = currentGenerated
			scaled := int(math.Round(float64(delta) * float64(s.cfg.rateIntervalBase) / float64(s.cfg.metricsInterval)))
			s.currentTPS.Store(int64(scaled))
		}
	}
}

func (s *SimulatorService) finishRun(run *simulationRun) {
	run.stopOnce.Do(run.cancel)
	s.currentTPS.Store(0)
	s.running.Store(false)

	s.stateMu.Lock()
	if s.run == run {
		s.run = nil
	}
	s.stateMu.Unlock()

	close(run.done)

	metrics := s.Metrics()
	s.logger.Info("simulation finished",
		"generated", metrics.TransactionsGenerated,
		"successful", metrics.SuccessfulTransactions,
		"failed", metrics.FailedTransactions,
	)
}

func (s *SimulatorService) resetMetrics() {
	s.generated.Store(0)
	s.successful.Store(0)
	s.failed.Store(0)
	s.currentTPS.Store(0)
}

func (s *SimulatorService) tokenInterval(tps int) time.Duration {
	interval := s.cfg.rateIntervalBase / time.Duration(tps)
	if interval <= 0 {
		return time.Nanosecond
	}
	return interval
}

type stoppableContext struct {
	done chan struct{}
}

func newStoppableContext() (*stoppableContext, func()) {
	done := make(chan struct{})
	var once sync.Once
	return &stoppableContext{done: done}, func() {
		once.Do(func() { close(done) })
	}
}

func (c *stoppableContext) Done() <-chan struct{} {
	return c.done
}

