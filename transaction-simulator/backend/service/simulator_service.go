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

// TransactionGenerator is the minimal contract required from the generator.
type TransactionGenerator interface {
	GenerateTimedSequence(mode generator.SimulationMode, opts generator.GenerateOptions) []generator.TimedTransaction
	GenerateScenario(scenario generator.ScenarioID, rules []generator.RuleType) []generator.TimedTransaction
}

// TransactionSender is the minimal contract required from the client.
type TransactionSender interface {
	SendTransaction(tx model.Transaction) error
}

// SimulationRequest describes one simulation run (traffic or scenario).
type SimulationRequest struct {
	Kind            generator.SimulationKind `json:"kind"`
	TPS             int                      `json:"tps"`
	Duration        int                      `json:"duration"`
	Mode            generator.SimulationMode `json:"mode"`
	Scenario        generator.ScenarioID     `json:"scenario"`
	Rules           []generator.RuleType     `json:"rules"`
	SourceType      *model.SourceType        `json:"sourceType"`
	FraudMixPercent *int                     `json:"fraudMixPercent"`
	FailedPercent   *int                     `json:"failedPercent"`
}

// SimulationMetrics is a thread-safe snapshot of the current or latest run.
type SimulationMetrics struct {
	Running                bool   `json:"running"`
	Kind                   string `json:"kind,omitempty"`
	Scenario               string `json:"scenario,omitempty"`
	Mode                   string `json:"mode,omitempty"`
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

// SimulatorService controls transaction generation, paced sending, and metrics.
type SimulatorService struct {
	generator TransactionGenerator
	sender    TransactionSender
	logger    *slog.Logger
	cfg       runtimeConfig

	generatorMu sync.Mutex
	stateMu     sync.Mutex
	run         *simulationRun

	generated  atomic.Uint64
	successful atomic.Uint64
	failed     atomic.Uint64
	currentTPS atomic.Int64
	running    atomic.Bool

	activeKind     atomic.Value // string
	activeScenario atomic.Value // string
	activeMode     atomic.Value // string
}

// NewSimulatorService creates a simulation engine.
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

	svc := &SimulatorService{
		generator: transactionGenerator,
		sender:    transactionClient,
		logger:    logger,
		cfg:       cfg,
	}
	svc.activeKind.Store("")
	svc.activeScenario.Store("")
	svc.activeMode.Store("")
	return svc, nil
}

// NewWithComponents is a convenience constructor for concrete packages.
func NewWithComponents(
	transactionGenerator *generator.Generator,
	transactionClient *client.TransactionClient,
	logger *slog.Logger,
) (*SimulatorService, error) {
	return NewSimulatorService(transactionGenerator, transactionClient, logger)
}

// Start begins a new simulation run (TRAFFIC or SCENARIO).
func (s *SimulatorService) Start(request SimulationRequest) error {
	request = normalizeRequest(request)
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
	s.activeKind.Store(string(request.Kind))
	s.activeScenario.Store(string(request.Scenario))
	s.activeMode.Store(string(request.Mode))
	s.stateMu.Unlock()

	if request.Kind == generator.KindScenario {
		go s.runScenario(ctx, run)
	} else {
		go s.runTraffic(ctx, run)
	}

	s.logger.Info("simulation started",
		"kind", string(request.Kind),
		"scenario", string(request.Scenario),
		"tps", request.TPS,
		"duration", request.Duration,
		"mode", string(request.Mode),
	)

	return nil
}

// Stop requests a graceful shutdown of the current simulation run.
func (s *SimulatorService) Stop() error {
	s.stateMu.Lock()
	run := s.run
	s.stateMu.Unlock()

	if run == nil {
		return nil
	}

	run.stopOnce.Do(func() {
		s.logger.Info("simulation stop requested",
			"kind", string(run.request.Kind),
			"scenario", string(run.request.Scenario),
		)
		run.cancel()
	})

	<-run.done
	return nil
}

// Metrics returns a consistent snapshot of the current metrics.
func (s *SimulatorService) Metrics() SimulationMetrics {
	kind, _ := s.activeKind.Load().(string)
	scenario, _ := s.activeScenario.Load().(string)
	mode, _ := s.activeMode.Load().(string)
	return SimulationMetrics{
		Running:                s.running.Load(),
		Kind:                   kind,
		Scenario:               scenario,
		Mode:                   mode,
		TransactionsGenerated:  s.generated.Load(),
		SuccessfulTransactions: s.successful.Load(),
		FailedTransactions:     s.failed.Load(),
		CurrentTPS:             int(s.currentTPS.Load()),
	}
}

func normalizeRequest(request SimulationRequest) SimulationRequest {
	if request.Kind == "" {
		request.Kind = generator.KindTraffic
	}
	return request
}

func (s *SimulatorService) validateRequest(request SimulationRequest) error {
	switch request.Kind {
	case generator.KindScenario:
		if request.Scenario == "" {
			return errors.New("simulator service: scenario is required for SCENARIO kind")
		}
		if !generator.IsValidScenario(request.Scenario) {
			return fmt.Errorf("simulator service: unsupported scenario %q", request.Scenario)
		}
		if request.Scenario == generator.ScenarioMultiRule {
			if _, err := generator.NormalizeMultiRules(request.Rules); err != nil {
				return fmt.Errorf("simulator service: %v", err)
			}
		}
		return nil
	case generator.KindTraffic:
		if request.TPS <= 0 {
			return errors.New("simulator service: tps must be greater than 0")
		}
		if request.Duration <= 0 {
			return errors.New("simulator service: duration must be greater than 0")
		}
		if request.Mode != generator.ModeNormal && request.Mode != generator.ModeFraud {
			return fmt.Errorf("simulator service: unsupported simulation mode %q", request.Mode)
		}
		if request.FraudMixPercent != nil {
			p := *request.FraudMixPercent
			if p < 0 || p > 100 {
				return errors.New("simulator service: fraudMixPercent must be between 0 and 100")
			}
		}
		if request.FailedPercent != nil {
			p := *request.FailedPercent
			if p < 0 || p > 100 {
				return errors.New("simulator service: failedPercent must be between 0 and 100")
			}
		}
		if request.SourceType != nil {
			st := *request.SourceType
			if st != model.SourceTypeBank && st != model.SourceTypeMerchant {
				return fmt.Errorf("simulator service: unsupported sourceType %q", st)
			}
		}
		return nil
	default:
		return fmt.Errorf("simulator service: unsupported kind %q", request.Kind)
	}
}

func (s *SimulatorService) runScenario(ctx *stoppableContext, run *simulationRun) {
	defer s.finishRun(run)

	s.generatorMu.Lock()
	var steps []generator.TimedTransaction
	if run.request.Scenario == generator.ScenarioMultiRule {
		rules, err := generator.NormalizeMultiRules(run.request.Rules)
		if err != nil {
			s.generatorMu.Unlock()
			s.logger.Warn("multi-rule validation failed at run", "error", err)
			return
		}
		steps = s.generator.GenerateScenario(run.request.Scenario, rules)
	} else {
		steps = s.generator.GenerateScenario(run.request.Scenario, nil)
	}
	s.generatorMu.Unlock()

	if len(steps) == 0 {
		s.logger.Warn("scenario produced no transactions", "scenario", string(run.request.Scenario))
		return
	}

	for _, step := range steps {
		if !s.waitDelay(ctx, step.DelayBefore) {
			return
		}
		s.generated.Add(1)
		s.sendOne(ctx, 0, step.Transaction)
	}
}

func (s *SimulatorService) runTraffic(ctx *stoppableContext, run *simulationRun) {
	request := run.request
	tokens := make(chan struct{}, s.cfg.generatorWorkers*2)
	transactions := make(chan model.Transaction, s.cfg.channelBuffer)

	var generatorWG sync.WaitGroup
	var senderWG sync.WaitGroup

	go s.runTokenProducer(ctx, request, tokens)

	for workerID := 0; workerID < s.cfg.generatorWorkers; workerID++ {
		generatorWG.Add(1)
		go s.runGeneratorWorker(ctx, request, workerID, tokens, transactions, &generatorWG)
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

	senderWG.Wait()
	s.finishRun(run)
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
	request SimulationRequest,
	workerID int,
	tokens <-chan struct{},
	transactions chan<- model.Transaction,
	wg *sync.WaitGroup,
) {
	defer wg.Done()

	opts := generator.GenerateOptions{SourceType: request.SourceType}

	for {
		select {
		case <-ctx.Done():
			return
		case _, ok := <-tokens:
			if !ok {
				return
			}

			mode := s.pickTrafficMode(request)

			s.generatorMu.Lock()
			steps := s.generator.GenerateTimedSequence(mode, opts)
			s.generatorMu.Unlock()

			for _, step := range steps {
				if !s.waitDelay(ctx, step.DelayBefore) {
					return
				}

				tx := step.Transaction
				s.applyFailedStatus(&tx, request.FailedPercent)

				s.generated.Add(1)
				s.logger.Debug("transaction generated",
					"worker", workerID,
					"accountId", tx.AccountID,
					"mode", string(mode),
					"status", string(tx.Status),
				)

				select {
				case <-ctx.Done():
					return
				case transactions <- tx:
				}
			}
		}
	}
}

func (s *SimulatorService) pickTrafficMode(request SimulationRequest) generator.SimulationMode {
	if request.Mode == generator.ModeFraud {
		return generator.ModeFraud
	}
	if request.FraudMixPercent == nil || *request.FraudMixPercent <= 0 {
		return generator.ModeNormal
	}
	s.generatorMu.Lock()
	// Use a cheap time-based roll without needing the generator's RNG.
	roll := int(time.Now().UnixNano()%100) + 1
	s.generatorMu.Unlock()
	if roll <= *request.FraudMixPercent {
		return generator.ModeFraud
	}
	return generator.ModeNormal
}

// applyFailedStatus marks the txn FAILED with roughly failedPercent probability.
// Scenario packs never call this — they stay COMPLETED for reliable rule demos.
func (s *SimulatorService) applyFailedStatus(tx *model.Transaction, failedPercent *int) {
	if tx == nil || failedPercent == nil || *failedPercent <= 0 {
		return
	}
	roll := int(time.Now().UnixNano()%100) + 1
	if roll <= *failedPercent {
		tx.Status = model.TransactionStatusFailed
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
			s.sendOne(ctx, workerID, tx)
		}
	}
}

func (s *SimulatorService) sendOne(ctx *stoppableContext, workerID int, tx model.Transaction) {
	select {
	case <-ctx.Done():
		return
	default:
	}

	if err := s.sender.SendTransaction(tx); err != nil {
		s.failed.Add(1)
		s.logger.Warn("transaction send failed",
			"worker", workerID,
			"accountId", tx.AccountID,
			"error", err,
		)
		return
	}

	s.successful.Add(1)
	s.logger.Debug("transaction sent",
		"worker", workerID,
		"accountId", tx.AccountID,
	)
}

func (s *SimulatorService) waitDelay(ctx *stoppableContext, delay time.Duration) bool {
	if delay <= 0 {
		select {
		case <-ctx.Done():
			return false
		default:
			return true
		}
	}
	timer := time.NewTimer(delay)
	defer timer.Stop()
	select {
	case <-ctx.Done():
		return false
	case <-timer.C:
		return true
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
		"kind", metrics.Kind,
		"scenario", metrics.Scenario,
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
