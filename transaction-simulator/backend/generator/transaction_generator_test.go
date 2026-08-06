package generator_test

import (
	"log/slog"
	"testing"
	"time"

	"transaction-simulator/generator"
	"transaction-simulator/model"
)

// ─── Helpers ──────────────────────────────────────────────────────────────────

// fixedNow returns a fixed timestamp so all tests are fully deterministic.
var fixedNow = time.Date(2026, 8, 6, 10, 30, 0, 0, time.UTC)

func fixedClock() time.Time { return fixedNow }

// newGen builds a Generator with a fixed seed and clock for reproducible tests.
func newGen(t *testing.T) *generator.Generator {
	t.Helper()
	return generator.New(42, slog.Default(), fixedClock)
}

// validSourceTypes is the complete set of allowed source type values.
var validSourceTypes = map[model.SourceType]bool{
	model.SourceTypeBank:     true,
	model.SourceTypeMerchant: true,
}

// validTxnTypes is the complete set of allowed transaction type values.
var validTxnTypes = map[model.TransactionType]bool{
	model.TransactionTypeDebit:    true,
	model.TransactionTypeCredit:   true,
	model.TransactionTypeTransfer: true,
}

// validStatuses is the complete set of allowed transaction status values.
var validStatuses = map[model.TransactionStatus]bool{
	model.TransactionStatusCompleted: true,
	model.TransactionStatusFailed:    true,
}

// ─── 1. Normal transaction generation ────────────────────────────────────────

func TestGenerate_Normal_RequiredFieldsPopulated(t *testing.T) {
	g := newGen(t)
	tx := g.Generate(generator.ModeNormal)

	if tx.SourceType == "" {
		t.Error("SourceType must not be empty")
	}
	if tx.SourceID == "" {
		t.Error("SourceID must not be empty")
	}
	if tx.SourceName == "" {
		t.Error("SourceName must not be empty")
	}
	if tx.AccountID == "" {
		t.Error("AccountID must not be empty")
	}
	if tx.PayeeID == "" {
		t.Error("PayeeID must not be empty")
	}
	if tx.Currency == "" {
		t.Error("Currency must not be empty")
	}
	if tx.Type == "" {
		t.Error("Type must not be empty")
	}
	if tx.Status == "" {
		t.Error("Status must not be empty")
	}
}

func TestGenerate_Normal_AmountGreaterThanZero(t *testing.T) {
	g := newGen(t)
	for i := range 20 {
		tx := g.Generate(generator.ModeNormal)
		if tx.Amount <= 0 {
			t.Errorf("trial %d: Amount must be > 0, got %f", i, tx.Amount)
		}
	}
}

func TestGenerate_Normal_AmountWithinNormalRange(t *testing.T) {
	g := newGen(t)
	for i := range 50 {
		tx := g.Generate(generator.ModeNormal)
		if tx.Amount < 10 || tx.Amount > 50_000 {
			t.Errorf("trial %d: normal amount %f out of expected range [10, 50000]", i, tx.Amount)
		}
	}
}

func TestGenerate_Normal_ValidEnums(t *testing.T) {
	g := newGen(t)
	for i := range 50 {
		tx := g.Generate(generator.ModeNormal)

		if !validSourceTypes[tx.SourceType] {
			t.Errorf("trial %d: invalid SourceType %q", i, tx.SourceType)
		}
		if !validTxnTypes[tx.Type] {
			t.Errorf("trial %d: invalid Type %q", i, tx.Type)
		}
		if !validStatuses[tx.Status] {
			t.Errorf("trial %d: invalid Status %q", i, tx.Status)
		}
	}
}

func TestGenerate_Normal_AmountRoundedToTwoDecimals(t *testing.T) {
	g := newGen(t)
	for i := range 30 {
		tx := g.Generate(generator.ModeNormal)
		rounded := float64(int(tx.Amount*100)) / 100
		if tx.Amount != rounded {
			t.Errorf("trial %d: amount %f is not rounded to 2 decimal places", i, tx.Amount)
		}
	}
}

func TestGenerate_Normal_StatusIsCompleted(t *testing.T) {
	g := newGen(t)
	for i := range 20 {
		tx := g.Generate(generator.ModeNormal)
		if tx.Status != model.TransactionStatusCompleted {
			t.Errorf("trial %d: normal transactions should have status COMPLETED, got %q", i, tx.Status)
		}
	}
}

// ─── 2. Fraud pattern — high amount ──────────────────────────────────────────

func TestGenerateFraudSequence_HighAmount_ReturnsSingleTransaction(t *testing.T) {
	g := newGen(t)
	txns := g.GenerateFraudSequence(generator.FraudPatternHighAmount)
	if len(txns) != 1 {
		t.Fatalf("FraudPatternHighAmount: want 1 transaction, got %d", len(txns))
	}
}

func TestGenerateFraudSequence_HighAmount_ExceedsThreshold(t *testing.T) {
	g := newGen(t)
	for i := range 20 {
		txns := g.GenerateFraudSequence(generator.FraudPatternHighAmount)
		if txns[0].Amount <= 100_000 {
			t.Errorf("trial %d: high-amount fraud amount %f must be > 100000", i, txns[0].Amount)
		}
	}
}

func TestGenerateFraudSequence_HighAmount_HasDedicatedAccountID(t *testing.T) {
	g := newGen(t)
	txns := g.GenerateFraudSequence(generator.FraudPatternHighAmount)
	if txns[0].AccountID != "ACC-FRAUD-AMT-001" {
		t.Errorf("high-amount fraud: want ACC-FRAUD-AMT-001, got %q", txns[0].AccountID)
	}
}

func TestGenerateFraudSequence_HighAmount_TypeIsTransfer(t *testing.T) {
	g := newGen(t)
	txns := g.GenerateFraudSequence(generator.FraudPatternHighAmount)
	if txns[0].Type != model.TransactionTypeTransfer {
		t.Errorf("high-amount fraud: want TRANSFER, got %q", txns[0].Type)
	}
}

// ─── 2. Fraud pattern — velocity ─────────────────────────────────────────────

func TestGenerateFraudSequence_Velocity_ReturnsMultipleTransactions(t *testing.T) {
	g := newGen(t)
	txns := g.GenerateFraudSequence(generator.FraudPatternVelocity)
	if len(txns) < 2 {
		t.Fatalf("velocity fraud: want at least 2 transactions, got %d", len(txns))
	}
}

func TestGenerateFraudSequence_Velocity_AllSameAccount(t *testing.T) {
	g := newGen(t)
	txns := g.GenerateFraudSequence(generator.FraudPatternVelocity)
	want := txns[0].AccountID
	for i, tx := range txns {
		if tx.AccountID != want {
			t.Errorf("velocity fraud: tx[%d] accountId %q differs from tx[0] %q",
				i, tx.AccountID, want)
		}
	}
}

func TestGenerateFraudSequence_Velocity_TimestampsIncreasing(t *testing.T) {
	g := newGen(t)
	txns := g.GenerateFraudSequence(generator.FraudPatternVelocity)
	for i := 1; i < len(txns); i++ {
		if !txns[i].Timestamp.After(txns[i-1].Timestamp.Time) {
			t.Errorf("velocity fraud: tx[%d] timestamp not after tx[%d]", i, i-1)
		}
	}
}

// ─── 2. Fraud pattern — impossible travel ────────────────────────────────────

func TestGenerateFraudSequence_ImpossibleTravel_ReturnsTwoTransactions(t *testing.T) {
	g := newGen(t)
	txns := g.GenerateFraudSequence(generator.FraudPatternImpossibleTravel)
	if len(txns) != 2 {
		t.Fatalf("impossible travel: want 2 transactions, got %d", len(txns))
	}
}

func TestGenerateFraudSequence_ImpossibleTravel_SameAccount(t *testing.T) {
	g := newGen(t)
	txns := g.GenerateFraudSequence(generator.FraudPatternImpossibleTravel)
	if txns[0].AccountID != txns[1].AccountID {
		t.Errorf("impossible travel: both transactions must share the same accountId; got %q and %q",
			txns[0].AccountID, txns[1].AccountID)
	}
}

func TestGenerateFraudSequence_ImpossibleTravel_DifferentLocations(t *testing.T) {
	g := newGen(t)
	txns := g.GenerateFraudSequence(generator.FraudPatternImpossibleTravel)

	if txns[0].Location == nil || txns[1].Location == nil {
		t.Fatal("impossible travel: both transactions must have a location set")
	}
	if *txns[0].Location == *txns[1].Location {
		t.Errorf("impossible travel: locations must differ; both are %q", *txns[0].Location)
	}
}

func TestGenerateFraudSequence_ImpossibleTravel_LondonThenNewYork(t *testing.T) {
	g := newGen(t)
	txns := g.GenerateFraudSequence(generator.FraudPatternImpossibleTravel)

	if txns[0].Location == nil || txns[1].Location == nil {
		t.Fatal("impossible travel: locations must not be nil")
	}
	if *txns[0].Location != "London" {
		t.Errorf("impossible travel: first leg must be London, got %q", *txns[0].Location)
	}
	if *txns[1].Location != "New York" {
		t.Errorf("impossible travel: second leg must be New York, got %q", *txns[1].Location)
	}
}

func TestGenerateFraudSequence_ImpossibleTravel_SecondAfterFirst(t *testing.T) {
	g := newGen(t)
	txns := g.GenerateFraudSequence(generator.FraudPatternImpossibleTravel)
	if !txns[1].Timestamp.After(txns[0].Timestamp.Time) {
		t.Error("impossible travel: second transaction timestamp must be after the first")
	}
}

func TestGenerateFraudSequence_ImpossibleTravel_GapIsSeconds(t *testing.T) {
	g := newGen(t)
	txns := g.GenerateFraudSequence(generator.FraudPatternImpossibleTravel)
	gap := txns[1].Timestamp.Sub(txns[0].Timestamp.Time)
	if gap > 10*time.Second {
		t.Errorf("impossible travel: gap %v should be only seconds to be suspicious", gap)
	}
}

// ─── 2. Fraud pattern — rapid frequency ──────────────────────────────────────

func TestGenerateFraudSequence_RapidFrequency_ReturnsManyTransactions(t *testing.T) {
	g := newGen(t)
	txns := g.GenerateFraudSequence(generator.FraudPatternRapidFrequency)
	if len(txns) < 5 {
		t.Fatalf("rapid frequency: want at least 5 transactions, got %d", len(txns))
	}
}

func TestGenerateFraudSequence_RapidFrequency_AllSameAccount(t *testing.T) {
	g := newGen(t)
	txns := g.GenerateFraudSequence(generator.FraudPatternRapidFrequency)
	want := txns[0].AccountID
	for i, tx := range txns {
		if tx.AccountID != want {
			t.Errorf("rapid frequency: tx[%d] accountId %q differs from tx[0] %q",
				i, tx.AccountID, want)
		}
	}
}

func TestGenerateFraudSequence_RapidFrequency_TimestampsIncreasing(t *testing.T) {
	g := newGen(t)
	txns := g.GenerateFraudSequence(generator.FraudPatternRapidFrequency)
	for i := 1; i < len(txns); i++ {
		if !txns[i].Timestamp.After(txns[i-1].Timestamp.Time) {
			t.Errorf("rapid frequency: tx[%d] timestamp not after tx[%d]", i, i-1)
		}
	}
}

func TestGenerateFraudSequence_RapidFrequency_SubSecondIntervals(t *testing.T) {
	g := newGen(t)
	txns := g.GenerateFraudSequence(generator.FraudPatternRapidFrequency)
	for i := 1; i < len(txns); i++ {
		gap := txns[i].Timestamp.Sub(txns[i-1].Timestamp.Time)
		if gap >= time.Second {
			t.Errorf("rapid frequency: gap between tx[%d] and tx[%d] is %v — expected < 1s",
				i-1, i, gap)
		}
	}
}

// ─── 3. Timestamp generation ──────────────────────────────────────────────────

func TestGenerate_Timestamp_UsesInjectedClock(t *testing.T) {
	g := generator.New(42, nil, fixedClock)
	tx := g.Generate(generator.ModeNormal)

	if !tx.Timestamp.Equal(fixedNow) {
		t.Errorf("timestamp: want %v, got %v", fixedNow, tx.Timestamp.Time)
	}
}

func TestGenerate_Timestamp_SerializesAsLocalDateTime(t *testing.T) {
	g := generator.New(42, nil, fixedClock)
	tx := g.Generate(generator.ModeNormal)

	formatted := tx.Timestamp.Format("2006-01-02T15:04:05")
	want := "2026-08-06T10:30:00"
	if formatted != want {
		t.Errorf("timestamp format: want %q, got %q", want, formatted)
	}
}

func TestGenerateFraudSequence_Velocity_TimestampBaseFromClock(t *testing.T) {
	g := generator.New(42, nil, fixedClock)
	txns := g.GenerateFraudSequence(generator.FraudPatternVelocity)

	// First tx in the velocity burst must start at the fixed clock time.
	if !txns[0].Timestamp.Equal(fixedNow) {
		t.Errorf("velocity: first tx timestamp want %v, got %v",
			fixedNow, txns[0].Timestamp.Time)
	}
}

func TestGenerateFraudSequence_ImpossibleTravel_TimestampBaseFromClock(t *testing.T) {
	g := generator.New(42, nil, fixedClock)
	txns := g.GenerateFraudSequence(generator.FraudPatternImpossibleTravel)

	if !txns[0].Timestamp.Equal(fixedNow) {
		t.Errorf("impossible travel: first tx timestamp want %v, got %v",
			fixedNow, txns[0].Timestamp.Time)
	}
}

// ─── GenerateBatch ────────────────────────────────────────────────────────────

func TestGenerateBatch_ReturnsCorrectCount(t *testing.T) {
	g := newGen(t)
	txns := g.GenerateBatch(generator.ModeNormal, 10)
	if len(txns) != 10 {
		t.Errorf("want 10 transactions, got %d", len(txns))
	}
}

func TestGenerateBatch_ZeroCount_ReturnsNil(t *testing.T) {
	g := newGen(t)
	txns := g.GenerateBatch(generator.ModeNormal, 0)
	if txns != nil {
		t.Errorf("want nil for count=0, got slice of length %d", len(txns))
	}
}

func TestGenerateBatch_NegativeCount_ReturnsNil(t *testing.T) {
	g := newGen(t)
	txns := g.GenerateBatch(generator.ModeNormal, -5)
	if txns != nil {
		t.Errorf("want nil for count=-5, got slice of length %d", len(txns))
	}
}

func TestGenerateBatch_FraudMode_AllTransactionsValid(t *testing.T) {
	g := newGen(t)
	txns := g.GenerateBatch(generator.ModeFraud, 20)
	for i, tx := range txns {
		if tx.AccountID == "" {
			t.Errorf("batch fraud tx[%d]: AccountID must not be empty", i)
		}
		if tx.Amount <= 0 {
			t.Errorf("batch fraud tx[%d]: Amount must be > 0, got %f", i, tx.Amount)
		}
		if !validStatuses[tx.Status] {
			t.Errorf("batch fraud tx[%d]: invalid Status %q", i, tx.Status)
		}
	}
}

// ─── Determinism ─────────────────────────────────────────────────────────────

func TestGenerate_SameSeed_ProducesIdenticalSequence(t *testing.T) {
	g1 := generator.New(99, nil, fixedClock)
	g2 := generator.New(99, nil, fixedClock)

	for i := range 10 {
		t1 := g1.Generate(generator.ModeNormal)
		t2 := g2.Generate(generator.ModeNormal)
		if t1.Amount != t2.Amount {
			t.Errorf("trial %d: same seed produced different amounts (%f vs %f)",
				i, t1.Amount, t2.Amount)
		}
		if t1.AccountID != t2.AccountID {
			t.Errorf("trial %d: same seed produced different accountIds (%q vs %q)",
				i, t1.AccountID, t2.AccountID)
		}
	}
}

func TestGenerate_DifferentSeeds_ProduceDifferentSequences(t *testing.T) {
	g1 := generator.New(1, nil, fixedClock)
	g2 := generator.New(2, nil, fixedClock)

	anyDiff := false
	for range 10 {
		t1 := g1.Generate(generator.ModeNormal)
		t2 := g2.Generate(generator.ModeNormal)
		if t1.Amount != t2.Amount || t1.AccountID != t2.AccountID {
			anyDiff = true
			break
		}
	}
	if !anyDiff {
		t.Error("different seeds produced identical sequences (astronomically unlikely)")
	}
}

// ─── Mode constants ───────────────────────────────────────────────────────────

func TestSimulationModeConstants(t *testing.T) {
	if string(generator.ModeNormal) != "NORMAL" {
		t.Errorf("ModeNormal: want NORMAL, got %q", generator.ModeNormal)
	}
	if string(generator.ModeFraud) != "FRAUD" {
		t.Errorf("ModeFraud: want FRAUD, got %q", generator.ModeFraud)
	}
}

