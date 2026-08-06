package generator

import (
	"fmt"
	"log/slog"
	"math"
	"math/rand"
	"time"

	"transaction-simulator/model"
)

// ─────────────────────────────────────────────────────────────────────────────
// Public types
// ─────────────────────────────────────────────────────────────────────────────

// SimulationMode controls the character of the transactions the Generator
// produces for continuous traffic runs.
type SimulationMode string

const (
	// ModeNormal produces realistic banking transactions under default rule thresholds.
	ModeNormal SimulationMode = "NORMAL"
	// ModeFraud produces transactions that exhibit rule-aligned suspicious patterns.
	ModeFraud SimulationMode = "FRAUD"
)

// SimulationKind distinguishes continuous traffic from one-shot demo packs.
type SimulationKind string

const (
	KindTraffic  SimulationKind = "TRAFFIC"
	KindScenario SimulationKind = "SCENARIO"
)

// ScenarioID identifies a deterministic demo pack.
type ScenarioID string

const (
	ScenarioAmountThreshold ScenarioID = "AMOUNT_THRESHOLD"
	ScenarioVelocity        ScenarioID = "VELOCITY"
	ScenarioNewPayee        ScenarioID = "NEW_PAYEE"
	ScenarioDailyLimit      ScenarioID = "DAILY_LIMIT"
	ScenarioSoftTenancyMix  ScenarioID = "SOFT_TENANCY_MIX"
	ScenarioMVPSeed         ScenarioID = "MVP_SEED"
)

// FraudPattern identifies the specific suspicious behaviour to simulate.
type FraudPattern int

const (
	// FraudPatternHighAmount — one transaction whose amount exceeds the amount threshold.
	FraudPatternHighAmount FraudPattern = iota

	// FraudPatternVelocity — five transactions from the same account, 1 s apart.
	FraudPatternVelocity

	// FraudPatternImpossibleTravel — two transactions London→NY (no matching rule;
	// kept for GenerateFraudSequence; excluded from random FRAUD traffic).
	FraudPatternImpossibleTravel

	// FraudPatternRapidFrequency — ten transactions, 100 ms apart (trips velocity).
	FraudPatternRapidFrequency

	// FraudPatternNewPayee — one transaction to a never-seen payee.
	FraudPatternNewPayee

	// FraudPatternDailyLimit — burst summing past the daily limit under amount threshold.
	FraudPatternDailyLimit

	fraudPatternCount
)

// ruleAlignedFraudPatterns are selected for continuous FRAUD traffic.
var ruleAlignedFraudPatterns = []FraudPattern{
	FraudPatternHighAmount,
	FraudPatternVelocity,
	FraudPatternRapidFrequency,
	FraudPatternNewPayee,
	FraudPatternDailyLimit,
}

// TimedTransaction is one emit step: optional delay, then the payload to POST.
type TimedTransaction struct {
	Transaction model.Transaction
	DelayBefore time.Duration
}

// GenerateOptions tunes continuous traffic generation.
type GenerateOptions struct {
	// SourceType, when set, forces every generated txn to that source type.
	SourceType *model.SourceType
	// Pattern, when set, forces a specific fraud pattern (FRAUD mode only).
	Pattern *FraudPattern
}

// Default thresholds mirrored from the monitoring monolith RuleEngineConfig.
const (
	defaultAmountThreshold = 10_000.0
	normalAmountMax        = 5_000.0
)

// ─────────────────────────────────────────────────────────────────────────────
// Static reference data
// ─────────────────────────────────────────────────────────────────────────────

type sourceRef struct {
	SourceType model.SourceType
	SourceID   string
	SourceName string
}

type payeeRef struct {
	ID   string
	Name string
}

type locationRef struct {
	Name      string
	Latitude  float64
	Longitude float64
}

var (
	knownSources = []sourceRef{
		{model.SourceTypeBank, "HSBC-UK", "HSBC United Kingdom"},
		{model.SourceTypeBank, "JPM-US", "JPMorgan Chase"},
		{model.SourceTypeMerchant, "ACME-POS", "ACME Payments"},
	}

	knownAccountIDs = []string{
		"ACC10001", "ACC10002", "ACC10003", "ACC10004", "ACC10005",
	}

	knownPayees = []payeeRef{
		{"PAYEE10001", "Amazon"},
		{"PAYEE10002", "Netflix"},
		{"PAYEE10003", "Walmart"},
		{"PAYEE10004", "Apple"},
		{"PAYEE10005", "Google"},
	}

	knownCurrencies = []string{"USD", "EUR", "INR"}

	knownTxnTypes = []model.TransactionType{
		model.TransactionTypeDebit,
		model.TransactionTypeCredit,
		model.TransactionTypeTransfer,
	}

	knownLocations = []locationRef{
		{"London", 51.5074, -0.1278},
		{"New York", 40.7128, -74.0060},
		{"Mumbai", 19.0760, 72.8777},
		{"Singapore", 1.3521, 103.8198},
	}
)

// IsValidScenario reports whether id is a known demo pack.
func IsValidScenario(id ScenarioID) bool {
	switch id {
	case ScenarioAmountThreshold, ScenarioVelocity, ScenarioNewPayee,
		ScenarioDailyLimit, ScenarioSoftTenancyMix, ScenarioMVPSeed:
		return true
	default:
		return false
	}
}

// ─────────────────────────────────────────────────────────────────────────────
// Generator
// ─────────────────────────────────────────────────────────────────────────────

// Generator produces model.Transaction values for simulation purposes.
//
// It is NOT safe for concurrent use. Create one Generator per goroutine, or
// protect shared access with a mutex.
type Generator struct {
	r      *rand.Rand
	logger *slog.Logger
	now    func() time.Time
	seq    uint64 // uniqueness for new-payee IDs
}

// New returns a Generator seeded with seed.
func New(seed int64, logger *slog.Logger, nowFn func() time.Time) *Generator {
	if logger == nil {
		logger = slog.Default()
	}
	if nowFn == nil {
		nowFn = time.Now
	}
	return &Generator{
		r:      rand.New(rand.NewSource(seed)), //nolint:gosec
		logger: logger,
		now:    nowFn,
	}
}

// Generate returns a single transaction for the given mode.
// Prefer GenerateTimedSequence for FRAUD so multi-txn patterns are not truncated.
func (g *Generator) Generate(mode SimulationMode) model.Transaction {
	seq := g.GenerateTimedSequence(mode, GenerateOptions{})
	if len(seq) == 0 {
		return g.generateNormal(GenerateOptions{})
	}
	return seq[0].Transaction
}

// GenerateBatch returns count independent traffic emissions (each may be the
// first leg of a fraud pattern only — use GenerateTimedSequence for full packs).
func (g *Generator) GenerateBatch(mode SimulationMode, count int) []model.Transaction {
	if count <= 0 {
		return nil
	}
	txns := make([]model.Transaction, count)
	for i := range txns {
		txns[i] = g.Generate(mode)
	}
	g.logger.Debug("generated batch", "mode", string(mode), "count", count)
	return txns
}

// GenerateTimedSequence returns one continuous-traffic emission as timed steps.
// NORMAL → single under-threshold txn. FRAUD → full rule-aligned pattern sequence.
func (g *Generator) GenerateTimedSequence(mode SimulationMode, opts GenerateOptions) []TimedTransaction {
	switch mode {
	case ModeFraud:
		pattern := g.pickFraudPattern(opts)
		raw := g.GenerateFraudSequence(pattern)
		return withDelaysForPattern(pattern, raw)
	default:
		tx := g.generateNormal(opts)
		g.logger.Debug("generated normal transaction",
			"accountId", tx.AccountID,
			"amount", tx.Amount,
		)
		return []TimedTransaction{{Transaction: tx}}
	}
}

// GenerateScenario builds a deterministic demo pack.
func (g *Generator) GenerateScenario(scenario ScenarioID) []TimedTransaction {
	switch scenario {
	case ScenarioAmountThreshold:
		return []TimedTransaction{{Transaction: g.scenarioAmountThreshold()}}
	case ScenarioVelocity:
		return g.scenarioVelocity()
	case ScenarioNewPayee:
		return []TimedTransaction{{Transaction: g.scenarioNewPayee()}}
	case ScenarioDailyLimit:
		return g.scenarioDailyLimit()
	case ScenarioSoftTenancyMix:
		return g.scenarioSoftTenancyMix()
	case ScenarioMVPSeed:
		return g.scenarioMVPSeed()
	default:
		g.logger.Warn("unknown scenario", "scenario", string(scenario))
		return nil
	}
}

// GenerateFraudSequence produces causally-related transactions for a pattern
// (without send delays — see withDelaysForPattern).
func (g *Generator) GenerateFraudSequence(pattern FraudPattern) []model.Transaction {
	switch pattern {
	case FraudPatternHighAmount:
		return []model.Transaction{g.generateHighAmount(GenerateOptions{})}
	case FraudPatternVelocity:
		return g.generateVelocitySequence(5, GenerateOptions{})
	case FraudPatternImpossibleTravel:
		pair := g.generateImpossibleTravelPair(GenerateOptions{})
		return []model.Transaction{pair[0], pair[1]}
	case FraudPatternRapidFrequency:
		return g.generateRapidSequence(10, GenerateOptions{})
	case FraudPatternNewPayee:
		return []model.Transaction{g.generateNewPayee(GenerateOptions{})}
	case FraudPatternDailyLimit:
		return g.generateDailyLimitBurst(GenerateOptions{})
	default:
		return []model.Transaction{g.generateHighAmount(GenerateOptions{})}
	}
}

func (g *Generator) pickFraudPattern(opts GenerateOptions) FraudPattern {
	if opts.Pattern != nil {
		return *opts.Pattern
	}
	return pick(g.r, ruleAlignedFraudPatterns)
}

func withDelaysForPattern(pattern FraudPattern, txns []model.Transaction) []TimedTransaction {
	out := make([]TimedTransaction, len(txns))
	for i, tx := range txns {
		var delay time.Duration
		if i > 0 {
			switch pattern {
			case FraudPatternVelocity:
				delay = time.Second
			case FraudPatternImpossibleTravel:
				delay = 2 * time.Second
			case FraudPatternRapidFrequency:
				delay = 100 * time.Millisecond
			case FraudPatternDailyLimit:
				delay = 50 * time.Millisecond
			default:
				delay = 0
			}
		}
		out[i] = TimedTransaction{Transaction: tx, DelayBefore: delay}
	}
	return out
}

// ─────────────────────────────────────────────────────────────────────────────
// Scenario builders
// ─────────────────────────────────────────────────────────────────────────────

func (g *Generator) scenarioAmountThreshold() model.Transaction {
	tx := g.generateNormal(GenerateOptions{})
	tx.AccountID = "ACC-SCENARIO-AMT-001"
	tx.Amount = g.roundedAmount(25_000, 75_000)
	tx.Type = model.TransactionTypeTransfer
	tx.Description = ptrOf("Scenario AMOUNT_THRESHOLD — over configured threshold")
	return tx
}

func (g *Generator) scenarioVelocity() []TimedTransaction {
	const count = 6
	base := g.now()
	out := make([]TimedTransaction, count)
	for i := range out {
		tx := g.generateNormal(GenerateOptions{})
		tx.AccountID = "ACC-SCENARIO-VEL-001"
		tx.Amount = g.roundedAmount(500, 2_000)
		tx.Timestamp = model.LocalDateTime{Time: base.Add(time.Duration(i) * 200 * time.Millisecond)}
		tx.Description = ptrOf("Scenario VELOCITY — burst over N in T minutes")
		delay := time.Duration(0)
		if i > 0 {
			delay = 100 * time.Millisecond
		}
		out[i] = TimedTransaction{Transaction: tx, DelayBefore: delay}
	}
	return out
}

func (g *Generator) scenarioNewPayee() model.Transaction {
	tx := g.generateNormal(GenerateOptions{})
	g.seq++
	tx.AccountID = "ACC-SCENARIO-PAY-001"
	tx.PayeeID = fmt.Sprintf("PAYEE-NEW-%d-%d", g.now().UnixNano(), g.seq)
	tx.PayeeName = ptrOf(fmt.Sprintf("Unseen Payee %d", g.seq))
	tx.Amount = g.roundedAmount(100, 2_000)
	tx.Description = ptrOf("Scenario NEW_PAYEE — first transaction to unseen payee")
	return tx
}

func (g *Generator) scenarioDailyLimit() []TimedTransaction {
	// 6 × 9000 = 54000 > default daily limit 50000; each under amount threshold.
	const count = 6
	const perTxn = 9_000.0
	base := g.now()
	out := make([]TimedTransaction, count)
	for i := range out {
		tx := g.generateNormal(GenerateOptions{})
		tx.AccountID = "ACC-SCENARIO-DAY-001"
		tx.Amount = perTxn
		tx.Timestamp = model.LocalDateTime{Time: base.Add(time.Duration(i) * time.Second)}
		tx.Description = ptrOf("Scenario DAILY_LIMIT — cumulative day spend")
		delay := time.Duration(0)
		if i > 0 {
			delay = 50 * time.Millisecond
		}
		out[i] = TimedTransaction{Transaction: tx, DelayBefore: delay}
	}
	return out
}

func (g *Generator) scenarioSoftTenancyMix() []TimedTransaction {
	bank := model.SourceTypeBank
	merchant := model.SourceTypeMerchant
	bankTx := g.generateNormal(GenerateOptions{SourceType: &bank})
	bankTx.AccountID = "ACC-SCENARIO-TEN-BANK"
	bankTx.Amount = g.roundedAmount(100, 2_000)
	bankTx.Description = ptrOf("Scenario SOFT_TENANCY_MIX — BANK normal")

	merchTx := g.generateNormal(GenerateOptions{SourceType: &merchant})
	merchTx.AccountID = "ACC-SCENARIO-TEN-MERCH"
	merchTx.Amount = g.roundedAmount(50, 500)
	merchTx.Description = ptrOf("Scenario SOFT_TENANCY_MIX — MERCHANT normal")

	return []TimedTransaction{
		{Transaction: bankTx},
		{Transaction: merchTx, DelayBefore: 50 * time.Millisecond},
	}
}

func (g *Generator) scenarioMVPSeed() []TimedTransaction {
	ts := g.now()
	bankNormal := model.Transaction{
		SourceType:  model.SourceTypeBank,
		SourceID:    "HSBC-UK",
		SourceName:  "HSBC United Kingdom",
		AccountID:   "ACC-1001",
		PayeeID:     "PAYEE-2001",
		PayeeName:   ptrOf("Acme Vendors Ltd"),
		Amount:      2500.00,
		Currency:    "INR",
		Type:        model.TransactionTypeTransfer,
		Timestamp:   model.LocalDateTime{Time: ts},
		Location:    ptrOf("London, UK"),
		Description: ptrOf("Normal vendor payment"),
		Status:      model.TransactionStatusCompleted,
	}
	merchNormal := model.Transaction{
		SourceType:  model.SourceTypeMerchant,
		SourceID:    "ACME-POS",
		SourceName:  "ACME Point of Sale",
		AccountID:   "ACC-2002",
		PayeeID:     "PAYEE-3001",
		PayeeName:   ptrOf("Corner Shop"),
		Amount:      149.99,
		Currency:    "INR",
		Type:        model.TransactionTypeDebit,
		Timestamp:   model.LocalDateTime{Time: ts},
		Location:    ptrOf("Mumbai, IN"),
		Description: ptrOf("POS purchase"),
		Status:      model.TransactionStatusCompleted,
	}
	over := model.Transaction{
		SourceType:  model.SourceTypeBank,
		SourceID:    "HSBC-UK",
		SourceName:  "HSBC United Kingdom",
		AccountID:   "ACC-1001",
		PayeeID:     "PAYEE-9999",
		PayeeName:   ptrOf("Suspicious Wire LLC"),
		Amount:      25000.00,
		Currency:    "INR",
		Type:        model.TransactionTypeTransfer,
		Timestamp:   model.LocalDateTime{Time: ts},
		Location:    ptrOf("London, UK"),
		Description: ptrOf("Over-threshold spike for demo"),
		Status:      model.TransactionStatusCompleted,
	}
	return []TimedTransaction{
		{Transaction: bankNormal},
		{Transaction: merchNormal, DelayBefore: 50 * time.Millisecond},
		{Transaction: over, DelayBefore: 50 * time.Millisecond},
	}
}

// ─────────────────────────────────────────────────────────────────────────────
// Normal / fraud builders
// ─────────────────────────────────────────────────────────────────────────────

func (g *Generator) generateNormal(opts GenerateOptions) model.Transaction {
	src := g.pickSource(opts)
	p := pick(g.r, knownPayees)
	loc := pick(g.r, knownLocations)

	return model.Transaction{
		SourceType:  src.SourceType,
		SourceID:    src.SourceID,
		SourceName:  src.SourceName,
		AccountID:   pick(g.r, knownAccountIDs),
		PayeeID:     p.ID,
		PayeeName:   ptrOf(p.Name),
		Amount:      g.roundedAmount(10, normalAmountMax),
		Currency:    pick(g.r, knownCurrencies),
		Type:        pick(g.r, knownTxnTypes),
		Timestamp:   model.LocalDateTime{Time: g.now()},
		Location:    ptrOf(loc.Name),
		Latitude:    ptrOf(loc.Latitude),
		Longitude:   ptrOf(loc.Longitude),
		Description: ptrOf("Regular payment"),
		Status:      model.TransactionStatusCompleted,
	}
}

func (g *Generator) pickSource(opts GenerateOptions) sourceRef {
	if opts.SourceType != nil {
		filtered := make([]sourceRef, 0, len(knownSources))
		for _, s := range knownSources {
			if s.SourceType == *opts.SourceType {
				filtered = append(filtered, s)
			}
		}
		if len(filtered) > 0 {
			return pick(g.r, filtered)
		}
	}
	return pick(g.r, knownSources)
}

func (g *Generator) generateHighAmount(opts GenerateOptions) model.Transaction {
	tx := g.generateNormal(opts)
	tx.AccountID = "ACC-FRAUD-AMT-001"
	// Align with monolith default amount threshold (10000), not 100000.
	tx.Amount = g.roundedAmount(defaultAmountThreshold+1, 999_999)
	tx.Type = model.TransactionTypeTransfer
	tx.Description = ptrOf("High-value transfer — amount threshold fraud pattern")
	return tx
}

func (g *Generator) generateVelocitySequence(count int, opts GenerateOptions) []model.Transaction {
	base := g.now()
	txns := make([]model.Transaction, count)
	for i := range txns {
		tx := g.generateNormal(opts)
		tx.AccountID = "ACC-FRAUD-VEL-001"
		tx.Amount = g.roundedAmount(500, 5_000)
		tx.Timestamp = model.LocalDateTime{Time: base.Add(time.Duration(i) * time.Second)}
		tx.Description = ptrOf("Velocity fraud — burst pattern")
		txns[i] = tx
	}
	return txns
}

func (g *Generator) generateImpossibleTravelPair(opts GenerateOptions) [2]model.Transaction {
	const accountID = "ACC-FRAUD-TRV-001"
	base := g.now()
	london := knownLocations[0]
	newYork := knownLocations[1]

	build := func(loc locationRef, when time.Time, desc string) model.Transaction {
		tx := g.generateNormal(opts)
		tx.AccountID = accountID
		tx.Timestamp = model.LocalDateTime{Time: when}
		tx.Location = ptrOf(loc.Name)
		tx.Latitude = ptrOf(loc.Latitude)
		tx.Longitude = ptrOf(loc.Longitude)
		tx.Description = ptrOf(desc)
		return tx
	}

	return [2]model.Transaction{
		build(london, base, "Impossible travel — London leg"),
		build(newYork, base.Add(2*time.Second), "Impossible travel — New York leg"),
	}
}

func (g *Generator) generateRapidSequence(count int, opts GenerateOptions) []model.Transaction {
	base := g.now()
	txns := make([]model.Transaction, count)
	for i := range txns {
		tx := g.generateNormal(opts)
		tx.AccountID = "ACC-FRAUD-RPD-001"
		tx.Amount = g.roundedAmount(100, 2_000)
		tx.Timestamp = model.LocalDateTime{Time: base.Add(time.Duration(i) * 100 * time.Millisecond)}
		tx.Description = ptrOf("Rapid frequency fraud — bot pattern")
		txns[i] = tx
	}
	return txns
}

func (g *Generator) generateNewPayee(opts GenerateOptions) model.Transaction {
	tx := g.generateNormal(opts)
	g.seq++
	tx.AccountID = "ACC-FRAUD-PAY-001"
	tx.PayeeID = fmt.Sprintf("PAYEE-NEW-%d-%d", g.now().UnixNano(), g.seq)
	tx.PayeeName = ptrOf(fmt.Sprintf("Unseen Payee %d", g.seq))
	tx.Amount = g.roundedAmount(100, 2_000)
	tx.Description = ptrOf("New payee fraud — unseen counterparty")
	return tx
}

func (g *Generator) generateDailyLimitBurst(opts GenerateOptions) []model.Transaction {
	const count = 6
	const perTxn = 9_000.0
	base := g.now()
	txns := make([]model.Transaction, count)
	for i := range txns {
		tx := g.generateNormal(opts)
		tx.AccountID = "ACC-FRAUD-DAY-001"
		tx.Amount = perTxn
		tx.Timestamp = model.LocalDateTime{Time: base.Add(time.Duration(i) * time.Second)}
		tx.Description = ptrOf("Daily limit fraud — cumulative burst")
		txns[i] = tx
	}
	return txns
}

// ─────────────────────────────────────────────────────────────────────────────
// Utilities
// ─────────────────────────────────────────────────────────────────────────────

func (g *Generator) roundedAmount(min, max float64) float64 {
	raw := min + g.r.Float64()*(max-min)
	cents := math.Round(raw * 100)
	return cents / 100
}

func pick[T any](r *rand.Rand, slice []T) T {
	return slice[r.Intn(len(slice))]
}

func ptrOf[T any](v T) *T {
	return &v
}
