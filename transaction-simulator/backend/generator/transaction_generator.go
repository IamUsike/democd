package generator

import (
	"fmt"
	"log/slog"
	"math"
	"math/rand"
	"strings"
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
	ScenarioMultiRule       ScenarioID = "MULTI_RULE"
)

// RuleType is a monitoring rule id that MULTI_RULE can target.
type RuleType string

const (
	RuleAmountThreshold RuleType = "AMOUNT_THRESHOLD"
	RuleVelocity        RuleType = "VELOCITY"
	RuleNewPayee        RuleType = "NEW_PAYEE"
	RuleDailyLimit      RuleType = "DAILY_LIMIT"
)

// DefaultMultiRules is used when MULTI_RULE is started without an explicit rules list.
var DefaultMultiRules = []RuleType{RuleAmountThreshold, RuleNewPayee}

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
	// QuietTPS sizes the NORMAL quiet account pool so default velocity
	// (5 txns / 10 min) is not tripped at this emit rate. Ignored for FRAUD.
	QuietTPS int
}

// Default thresholds mirrored from the monitoring monolith RuleEngineConfig.
const (
	defaultAmountThreshold = 10_000.0
	normalAmountMax        = 5_000.0

	// Quiet NORMAL stays under default VelocityRule (fires when count > 5 in 10 min).
	defaultVelocityMaxInWindow = 5
	defaultVelocityWindowSec   = 10 * 60
	// Must match Flyway V6 quiet-history seed size (ACC-QUIET-00000 ..).
	quietAccountPoolMax = 7200
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
		{model.SourceTypeBank, "BARCLAYS-UK", "Barclays"},
		{model.SourceTypeBank, "DBS-SG", "DBS Bank Singapore"},
		{model.SourceTypeBank, "HDFC-IN", "HDFC Bank"},
		{model.SourceTypeBank, "Citi-US", "Citibank"},
		{model.SourceTypeMerchant, "ACME-POS", "ACME Payments"},
		{model.SourceTypeMerchant, "STRIPE-EU", "Stripe Europe"},
		{model.SourceTypeMerchant, "SQUARE-US", "Square"},
		{model.SourceTypeMerchant, "RAZORPAY-IN", "Razorpay"},
		{model.SourceTypeMerchant, "SHOPIFY-CA", "Shopify Payments"},
	}

	knownAccountIDs = []string{
		"ACC10001", "ACC10002", "ACC10003", "ACC10004", "ACC10005",
		"ACC10006", "ACC10007", "ACC10008", "ACC10009", "ACC10010",
		"ACC20011", "ACC20012", "ACC20013", "ACC20014", "ACC20015",
		"ACC30021", "ACC30022", "ACC30023", "ACC30024", "ACC30025",
	}

	knownPayees = []payeeRef{
		{"PAYEE10001", "Amazon"},
		{"PAYEE10002", "Netflix"},
		{"PAYEE10003", "Walmart"},
		{"PAYEE10004", "Apple"},
		{"PAYEE10005", "Google"},
		{"PAYEE10006", "Spotify"},
		{"PAYEE10007", "Uber"},
		{"PAYEE10008", "Starbucks"},
		{"PAYEE10009", "IKEA"},
		{"PAYEE10010", "Tesla"},
		{"PAYEE10011", "Microsoft"},
		{"PAYEE10012", "Adobe"},
		{"PAYEE10013", "Shopify"},
		{"PAYEE10014", "Target"},
		{"PAYEE10015", "Costco"},
		{"PAYEE10016", "Airbnb"},
		{"PAYEE10017", "Delta Airlines"},
		{"PAYEE10018", "Shell Fuel"},
		{"PAYEE10019", "Whole Foods"},
		{"PAYEE10020", "Best Buy"},
		{"PAYEE10021", "PayPal Transfer"},
		{"PAYEE10022", "Wise FX"},
		{"PAYEE10023", "Local Bakery Co"},
		{"PAYEE10024", "Metro Transit"},
		{"PAYEE10025", "City Utilities"},
	}

	// Fragments for synthetic “new payee” display names (not in knownPayees).
	newPayeePrefixes = []string{
		"Nova", "Apex", "Bright", "Cedar", "Delta", "Echo", "Forge", "Grove",
		"Harbor", "Ivory", "Jade", "Kite", "Lumen", "Maple", "North", "Orbit",
		"Prism", "Quill", "Ridge", "Summit",
	}
	newPayeeSuffixes = []string{
		"Labs", "Trading", "Goods", "Services", "Holdings", "Retail",
		"Partners", "Ventures", "Supply", "Group", "LLC", "Ltd",
	}

	knownCurrencies = []string{"USD", "EUR", "INR", "GBP", "SGD", "AED"}

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
		{"Frankfurt", 50.1109, 8.6821},
		{"Tokyo", 35.6762, 139.6503},
		{"Sydney", -33.8688, 151.2093},
		{"Toronto", 43.6532, -79.3832},
		{"Dubai", 25.2048, 55.2708},
		{"São Paulo", -23.5505, -46.6333},
		{"Berlin", 52.5200, 13.4050},
		{"Chicago", 41.8781, -87.6298},
	}

	knownDescriptions = []string{
		"Regular payment",
		"Card purchase",
		"Online order",
		"Subscription renewal",
		"ATM withdrawal",
		"Wire transfer",
		"POS sale",
		"Bill payment",
		"Payroll credit",
		"Refund",
		"Peer transfer",
		"Travel booking",
	}
)

// IsValidScenario reports whether id is a known demo pack.
func IsValidScenario(id ScenarioID) bool {
	switch id {
	case ScenarioAmountThreshold, ScenarioVelocity, ScenarioNewPayee,
		ScenarioDailyLimit, ScenarioSoftTenancyMix, ScenarioMVPSeed,
		ScenarioMultiRule:
		return true
	default:
		return false
	}
}

// IsValidRuleType reports whether id is a selectable monitoring rule.
func IsValidRuleType(id RuleType) bool {
	switch id {
	case RuleAmountThreshold, RuleVelocity, RuleNewPayee, RuleDailyLimit:
		return true
	default:
		return false
	}
}

// NormalizeMultiRules deduplicates and validates a MULTI_RULE selection.
// Empty input returns DefaultMultiRules. Requires at least two distinct rules.
func NormalizeMultiRules(rules []RuleType) ([]RuleType, error) {
	if len(rules) == 0 {
		return append([]RuleType(nil), DefaultMultiRules...), nil
	}
	seen := make(map[RuleType]bool, len(rules))
	out := make([]RuleType, 0, len(rules))
	for _, r := range rules {
		r = RuleType(strings.ToUpper(strings.TrimSpace(string(r))))
		if !IsValidRuleType(r) {
			return nil, fmt.Errorf("unsupported rule %q", r)
		}
		if seen[r] {
			continue
		}
		seen[r] = true
		out = append(out, r)
	}
	if len(out) < 2 {
		return nil, fmt.Errorf("multi-rule requires at least 2 distinct rules, got %d", len(out))
	}
	return out, nil
}

// ─────────────────────────────────────────────────────────────────────────────
// Generator
// ─────────────────────────────────────────────────────────────────────────────

// QuietAccountCount returns how many distinct ACC-QUIET-* accounts NORMAL
// traffic should rotate through so a sustained `tps` stays at or under the
// default velocity ceiling (5 txns / 10 minutes per account).
func QuietAccountCount(tps int) int {
	if tps < 1 {
		tps = 1
	}
	n := (tps * defaultVelocityWindowSec) / defaultVelocityMaxInWindow
	if (tps*defaultVelocityWindowSec)%defaultVelocityMaxInWindow != 0 {
		n++
	}
	if n < 50 {
		n = 50
	}
	if n > quietAccountPoolMax {
		n = quietAccountPoolMax
	}
	return n
}

// Generator produces model.Transaction values for simulation purposes.
//
// It is NOT safe for concurrent use. Create one Generator per goroutine, or
// protect shared access with a mutex.
type Generator struct {
	r        *rand.Rand
	logger   *slog.Logger
	now      func() time.Time
	seq      uint64 // uniqueness for new-payee IDs
	quietSeq uint64 // round-robin index for quiet NORMAL accounts
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
		tx := g.generateQuietNormal(opts)
		g.logger.Debug("generated quiet normal transaction",
			"accountId", tx.AccountID,
			"amount", tx.Amount,
			"quietTps", opts.QuietTPS,
		)
		return []TimedTransaction{{Transaction: tx}}
	}
}

// GenerateScenario builds a deterministic demo pack.
// For MULTI_RULE, pass the selected rules (nil/empty → Amount + New Payee).
func (g *Generator) GenerateScenario(scenario ScenarioID, rules []RuleType) []TimedTransaction {
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
	case ScenarioMultiRule:
		normalized, err := NormalizeMultiRules(rules)
		if err != nil {
			g.logger.Warn("invalid multi-rule selection", "error", err)
			return nil
		}
		return g.scenarioMultiRule(normalized)
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

// scenarioMultiRule builds a sequence that should trip every selected rule.
func (g *Generator) scenarioMultiRule(rules []RuleType) []TimedTransaction {
	want := map[RuleType]bool{}
	for _, r := range rules {
		want[r] = true
	}

	needBurst := want[RuleVelocity] || want[RuleDailyLimit]
	count := 1
	if needBurst {
		count = 6
	}

	base := g.now()
	accountID := "ACC-SCENARIO-MULTI-001"
	payeeID := "PAYEE10001"
	payeeName := "Amazon"
	if want[RuleNewPayee] {
		payeeID, payeeName = g.inventNewPayee()
	}

	out := make([]TimedTransaction, count)
	for i := range out {
		tx := g.generateNormal(GenerateOptions{})
		tx.AccountID = accountID
		tx.PayeeID = payeeID
		tx.PayeeName = ptrOf(payeeName)
		tx.Type = model.TransactionTypeTransfer
		tx.Timestamp = model.LocalDateTime{Time: base.Add(time.Duration(i) * 150 * time.Millisecond)}
		tx.Description = ptrOf(fmt.Sprintf("Scenario MULTI_RULE — %v", rules))
		tx.Amount = multiRuleAmount(want, i, count, g)

		delay := time.Duration(0)
		if i > 0 {
			delay = 100 * time.Millisecond
		}
		out[i] = TimedTransaction{Transaction: tx, DelayBefore: delay}
	}
	return out
}

func multiRuleAmount(want map[RuleType]bool, index, count int, g *Generator) float64 {
	switch {
	case want[RuleDailyLimit] && want[RuleAmountThreshold]:
		// Each leg over amount threshold; sum (6×12k) exceeds daily limit.
		return 12_000
	case want[RuleDailyLimit]:
		// Stay under amount threshold while summing past 50k.
		return 9_000
	case want[RuleAmountThreshold] && count == 1:
		return g.roundedAmount(25_000, 75_000)
	case want[RuleAmountThreshold] && index == count-1:
		// Quiet legs first (velocity), then a high-value final txn.
		return g.roundedAmount(25_000, 75_000)
	default:
		return g.roundedAmount(500, 2_000)
	}
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

func (g *Generator) inventNewPayee() (id string, name string) {
	g.seq++
	id = fmt.Sprintf("PAYEE-NEW-%d-%d", g.now().UnixNano(), g.seq)
	name = fmt.Sprintf("%s %s %d",
		pick(g.r, newPayeePrefixes),
		pick(g.r, newPayeeSuffixes),
		g.seq%1000,
	)
	return id, name
}

func (g *Generator) scenarioNewPayee() model.Transaction {
	tx := g.generateNormal(GenerateOptions{})
	payeeID, payeeName := g.inventNewPayee()
	tx.AccountID = "ACC-SCENARIO-PAY-001"
	tx.PayeeID = payeeID
	tx.PayeeName = ptrOf(payeeName)
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
	ts := g.now()
	// Fixed account↔payee pairs match Flyway V6 seed history so NEW_PAYEE
	// does not fire; separate accounts so a single run cannot trip velocity.
	bankTx := model.Transaction{
		SourceType:  model.SourceTypeBank,
		SourceID:    "HSBC-UK",
		SourceName:  "HSBC United Kingdom",
		AccountID:   "ACC-SCENARIO-TEN-BANK",
		PayeeID:     "PAYEE10001",
		PayeeName:   ptrOf("Amazon"),
		Amount:      250.00,
		Currency:    "USD",
		Type:        model.TransactionTypeTransfer,
		Timestamp:   model.LocalDateTime{Time: ts},
		Location:    ptrOf("London"),
		Description: ptrOf("Scenario SOFT_TENANCY_MIX — BANK normal"),
		Status:      model.TransactionStatusCompleted,
	}
	merchTx := model.Transaction{
		SourceType:  model.SourceTypeMerchant,
		SourceID:    "ACME-POS",
		SourceName:  "ACME Payments",
		AccountID:   "ACC-SCENARIO-TEN-MERCH",
		PayeeID:     "PAYEE10002",
		PayeeName:   ptrOf("Netflix"),
		Amount:      49.99,
		Currency:    "USD",
		Type:        model.TransactionTypeTransfer,
		Timestamp:   model.LocalDateTime{Time: ts},
		Location:    ptrOf("Mumbai"),
		Description: ptrOf("Scenario SOFT_TENANCY_MIX — MERCHANT normal"),
		Status:      model.TransactionStatusCompleted,
	}
	return []TimedTransaction{
		{Transaction: bankTx},
		{Transaction: merchTx, DelayBefore: 50 * time.Millisecond},
	}
}

func (g *Generator) scenarioMVPSeed() []TimedTransaction {
	ts := g.now()
	// Quiet legs reuse V6-seeded account↔payee pairs (no NEW_PAYEE).
	// Over-threshold reuses the same bank payee so only AMOUNT_THRESHOLD fires.
	bankNormal := model.Transaction{
		SourceType:  model.SourceTypeBank,
		SourceID:    "HSBC-UK",
		SourceName:  "HSBC United Kingdom",
		AccountID:   "ACC-1001",
		PayeeID:     "PAYEE10001",
		PayeeName:   ptrOf("Amazon"),
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
		PayeeID:     "PAYEE10002",
		PayeeName:   ptrOf("Netflix"),
		Amount:      149.99,
		Currency:    "INR",
		Type:        model.TransactionTypeTransfer,
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
		PayeeID:     "PAYEE10001",
		PayeeName:   ptrOf("Amazon"),
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
		Description: ptrOf(pick(g.r, knownDescriptions)),
		Status:      model.TransactionStatusCompleted,
	}
}

// generateQuietNormal builds continuous NORMAL traffic that stays under default
// amount / velocity / daily-limit rules when V6 quiet-history seed is present:
// large ACC-QUIET-* pool, stable payee per account, TRANSFER (daily limit is DEBIT-only).
func (g *Generator) generateQuietNormal(opts GenerateOptions) model.Transaction {
	src := g.pickSource(opts)
	loc := pick(g.r, knownLocations)

	n := QuietAccountCount(opts.QuietTPS)
	idx := int(g.quietSeq % uint64(n))
	g.quietSeq++

	p := knownPayees[idx%len(knownPayees)]

	return model.Transaction{
		SourceType:  src.SourceType,
		SourceID:    src.SourceID,
		SourceName:  src.SourceName,
		AccountID:   fmt.Sprintf("ACC-QUIET-%05d", idx),
		PayeeID:     p.ID,
		PayeeName:   ptrOf(p.Name),
		Amount:      g.roundedAmount(10, normalAmountMax),
		Currency:    pick(g.r, knownCurrencies),
		Type:        model.TransactionTypeTransfer,
		Timestamp:   model.LocalDateTime{Time: g.now()},
		Location:    ptrOf(loc.Name),
		Latitude:    ptrOf(loc.Latitude),
		Longitude:   ptrOf(loc.Longitude),
		Description: ptrOf("Quiet normal traffic"),
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
	payeeID, payeeName := g.inventNewPayee()
	tx.AccountID = "ACC-FRAUD-PAY-001"
	tx.PayeeID = payeeID
	tx.PayeeName = ptrOf(payeeName)
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
