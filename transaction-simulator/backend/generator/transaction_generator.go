package generator

import (
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
// produces.
type SimulationMode string

const (
	// ModeNormal produces realistic banking transactions within normal ranges.
	ModeNormal SimulationMode = "NORMAL"
	// ModeFraud produces transactions that exhibit one of the supported
	// suspicious patterns.
	ModeFraud SimulationMode = "FRAUD"
)

// FraudPattern identifies the specific suspicious behaviour to simulate.
type FraudPattern int

const (
	// FraudPatternHighAmount — one transaction whose amount exceeds 100 000.
	FraudPatternHighAmount FraudPattern = iota

	// FraudPatternVelocity — five transactions from the same account,
	// each one second apart.
	FraudPatternVelocity

	// FraudPatternImpossibleTravel — two transactions from the same account:
	// one in London, one in New York, only two seconds apart.
	FraudPatternImpossibleTravel

	// FraudPatternRapidFrequency — ten transactions from the same account,
	// each 100 ms apart.
	FraudPatternRapidFrequency

	// fraudPatternCount is a sentinel used by Generate to bound random selection.
	fraudPatternCount
)

// ─────────────────────────────────────────────────────────────────────────────
// Static reference data (read-only; no global mutable state)
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

	// knownLocations is indexed directly for impossible travel:
	//   index 0 = London, index 1 = New York
	knownLocations = []locationRef{
		{"London", 51.5074, -0.1278},
		{"New York", 40.7128, -74.0060},
		{"Mumbai", 19.0760, 72.8777},
		{"Singapore", 1.3521, 103.8198},
	}
)

// ─────────────────────────────────────────────────────────────────────────────
// Generator
// ─────────────────────────────────────────────────────────────────────────────

// Generator produces model.Transaction values for simulation purposes.
//
// It is NOT safe for concurrent use. Create one Generator per goroutine, or
// protect shared access with a mutex.
type Generator struct {
	r      *rand.Rand       // private source; never shared
	logger *slog.Logger
	now    func() time.Time // injectable clock — overrideable in tests
}

// New returns a Generator seeded with seed.
//
//   - logger: pass nil to fall back to slog.Default()
//   - nowFn:  pass nil to use time.Now; inject a fixed function in unit tests
//     to make timestamps deterministic
func New(seed int64, logger *slog.Logger, nowFn func() time.Time) *Generator {
	if logger == nil {
		logger = slog.Default()
	}
	if nowFn == nil {
		nowFn = time.Now
	}
	return &Generator{
		r:      rand.New(rand.NewSource(seed)), //nolint:gosec // not crypto; deterministic seed intentional
		logger: logger,
		now:    nowFn,
	}
}

// ─────────────────────────────────────────────────────────────────────────────
// Public API
// ─────────────────────────────────────────────────────────────────────────────

// Generate returns a single transaction for the given mode.
//
// In ModeFraud, a fraud pattern is chosen at random on every call. Use
// GenerateFraudSequence when you need a specific pattern or a causally-related
// set of transactions.
func (g *Generator) Generate(mode SimulationMode) model.Transaction {
	switch mode {
	case ModeFraud:
		pattern := FraudPattern(g.r.Intn(int(fraudPatternCount)))
		txns := g.GenerateFraudSequence(pattern)
		tx := txns[0]
		g.logger.Debug("generated fraud transaction",
			"pattern", int(pattern),
			"accountId", tx.AccountID,
			"amount", tx.Amount,
		)
		return tx
	default:
		tx := g.generateNormal()
		g.logger.Debug("generated normal transaction",
			"accountId", tx.AccountID,
			"amount", tx.Amount,
		)
		return tx
	}
}

// GenerateBatch returns count transactions all in the same mode.
// Returns nil when count ≤ 0.
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

// GenerateFraudSequence produces a slice of causally-related transactions for
// the requested pattern.
//
//   - FraudPatternHighAmount       → 1 transaction, amount > 100 000
//   - FraudPatternVelocity         → 5 transactions from ACC-FRAUD-VEL-001, 1 s apart
//   - FraudPatternImpossibleTravel → 2 transactions (London → New York, 2 s apart)
//   - FraudPatternRapidFrequency   → 10 transactions from ACC-FRAUD-RPD-001, 100 ms apart
func (g *Generator) GenerateFraudSequence(pattern FraudPattern) []model.Transaction {
	switch pattern {
	case FraudPatternHighAmount:
		return []model.Transaction{g.generateHighAmount()}
	case FraudPatternVelocity:
		return g.generateVelocitySequence(5)
	case FraudPatternImpossibleTravel:
		pair := g.generateImpossibleTravelPair()
		return []model.Transaction{pair[0], pair[1]}
	case FraudPatternRapidFrequency:
		return g.generateRapidSequence(10)
	default:
		return []model.Transaction{g.generateHighAmount()}
	}
}

// ─────────────────────────────────────────────────────────────────────────────
// Normal transaction builder
// ─────────────────────────────────────────────────────────────────────────────

func (g *Generator) generateNormal() model.Transaction {
	src := pick(g.r, knownSources)
	p := pick(g.r, knownPayees)
	loc := pick(g.r, knownLocations)

	return model.Transaction{
		SourceType:  src.SourceType,
		SourceID:    src.SourceID,
		SourceName:  src.SourceName,
		AccountID:   pick(g.r, knownAccountIDs),
		PayeeID:     p.ID,
		PayeeName:   ptrOf(p.Name),
		Amount:      g.roundedAmount(10, 50_000),
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

// ─────────────────────────────────────────────────────────────────────────────
// Fraud pattern builders
// ─────────────────────────────────────────────────────────────────────────────

// generateHighAmount returns one transaction whose amount exceeds 100 000,
// originating from a dedicated fraud sentinel account.
func (g *Generator) generateHighAmount() model.Transaction {
	tx := g.generateNormal()
	tx.AccountID = "ACC-FRAUD-AMT-001"
	tx.Amount = g.roundedAmount(100_001, 999_999)
	tx.Type = model.TransactionTypeTransfer
	tx.Description = ptrOf("High-value transfer — amount threshold fraud pattern")
	return tx
}

// generateVelocitySequence returns count transactions from the same fraud
// account, each spaced one second apart from a common base time.
func (g *Generator) generateVelocitySequence(count int) []model.Transaction {
	base := g.now()
	txns := make([]model.Transaction, count)
	for i := range txns {
		tx := g.generateNormal()
		tx.AccountID = "ACC-FRAUD-VEL-001"
		tx.Amount = g.roundedAmount(500, 5_000)
		tx.Timestamp = model.LocalDateTime{Time: base.Add(time.Duration(i) * time.Second)}
		tx.Description = ptrOf("Velocity fraud — burst pattern")
		txns[i] = tx
	}
	return txns
}

// generateImpossibleTravelPair returns exactly two transactions from the same
// account: one in London (index 0) and one in New York (index 1), only two
// seconds apart — physically impossible travel time.
func (g *Generator) generateImpossibleTravelPair() [2]model.Transaction {
	const accountID = "ACC-FRAUD-TRV-001"
	base := g.now()
	london := knownLocations[0] // always London
	newYork := knownLocations[1] // always New York

	build := func(loc locationRef, when time.Time, desc string) model.Transaction {
		tx := g.generateNormal()
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

// generateRapidSequence returns count transactions from the same fraud account,
// each spaced 100 ms apart — simulating automation-driven rapid-fire activity.
func (g *Generator) generateRapidSequence(count int) []model.Transaction {
	base := g.now()
	txns := make([]model.Transaction, count)
	for i := range txns {
		tx := g.generateNormal()
		tx.AccountID = "ACC-FRAUD-RPD-001"
		tx.Amount = g.roundedAmount(100, 2_000)
		tx.Timestamp = model.LocalDateTime{Time: base.Add(time.Duration(i) * 100 * time.Millisecond)}
		tx.Description = ptrOf("Rapid frequency fraud — bot pattern")
		txns[i] = tx
	}
	return txns
}

// ─────────────────────────────────────────────────────────────────────────────
// Utility functions
// ─────────────────────────────────────────────────────────────────────────────

// roundedAmount returns a random float64 in [min, max] rounded to 2 decimal
// places, matching the DECIMAL(15,2) column type.
func (g *Generator) roundedAmount(min, max float64) float64 {
	raw := min + g.r.Float64()*(max-min)
	return math.Round(raw*100) / 100
}

// pick returns a uniformly random element from slice using r.
// Uses Go 1.18+ generics — no interface boxing.
func pick[T any](r *rand.Rand, slice []T) T {
	return slice[r.Intn(len(slice))]
}

// ptrOf returns a pointer to a copy of v.
// Eliminates the repetitive "x := value; &x" pattern throughout builders.
func ptrOf[T any](v T) *T {
	return &v
}

