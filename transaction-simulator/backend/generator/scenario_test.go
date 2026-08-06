package generator_test

import (
	"strings"
	"testing"
	"time"

	"transaction-simulator/generator"
	"transaction-simulator/model"
)

func TestGenerateScenario_AmountThreshold(t *testing.T) {
	g := newGen(t)
	seq := g.GenerateScenario(generator.ScenarioAmountThreshold)
	if len(seq) != 1 {
		t.Fatalf("AMOUNT_THRESHOLD: want 1 txn, got %d", len(seq))
	}
	tx := seq[0].Transaction
	if tx.Amount <= 10_000 {
		t.Errorf("AMOUNT_THRESHOLD: amount %f must be > 10000", tx.Amount)
	}
	if tx.AccountID != "ACC-SCENARIO-AMT-001" {
		t.Errorf("AMOUNT_THRESHOLD: want ACC-SCENARIO-AMT-001, got %q", tx.AccountID)
	}
}

func TestGenerateScenario_Velocity(t *testing.T) {
	g := newGen(t)
	seq := g.GenerateScenario(generator.ScenarioVelocity)
	if len(seq) < 6 {
		t.Fatalf("VELOCITY: want at least 6 txns (max default 5), got %d", len(seq))
	}
	account := seq[0].Transaction.AccountID
	for i, step := range seq {
		if step.Transaction.AccountID != account {
			t.Errorf("VELOCITY: tx[%d] account %q != %q", i, step.Transaction.AccountID, account)
		}
		if step.Transaction.Amount > 10_000 {
			t.Errorf("VELOCITY: tx[%d] amount %f should stay under amount threshold", i, step.Transaction.Amount)
		}
	}
	if seq[1].DelayBefore <= 0 {
		t.Error("VELOCITY: subsequent steps should have DelayBefore > 0")
	}
}

func TestGenerateScenario_NewPayee(t *testing.T) {
	g := newGen(t)
	seq := g.GenerateScenario(generator.ScenarioNewPayee)
	if len(seq) != 1 {
		t.Fatalf("NEW_PAYEE: want 1 txn, got %d", len(seq))
	}
	tx := seq[0].Transaction
	if !strings.HasPrefix(tx.PayeeID, "PAYEE-NEW-") {
		t.Errorf("NEW_PAYEE: payeeId %q should start with PAYEE-NEW-", tx.PayeeID)
	}
	if tx.Amount > 10_000 {
		t.Errorf("NEW_PAYEE: amount %f should stay under amount threshold", tx.Amount)
	}
}

func TestGenerateScenario_DailyLimit(t *testing.T) {
	g := newGen(t)
	seq := g.GenerateScenario(generator.ScenarioDailyLimit)
	if len(seq) < 2 {
		t.Fatalf("DAILY_LIMIT: want multiple txns, got %d", len(seq))
	}
	var sum float64
	account := seq[0].Transaction.AccountID
	for i, step := range seq {
		sum += step.Transaction.Amount
		if step.Transaction.AccountID != account {
			t.Errorf("DAILY_LIMIT: tx[%d] account mismatch", i)
		}
		if step.Transaction.Amount > 10_000 {
			t.Errorf("DAILY_LIMIT: tx[%d] amount %f should stay under amount threshold", i, step.Transaction.Amount)
		}
	}
	if sum <= 50_000 {
		t.Errorf("DAILY_LIMIT: sum %f must exceed default daily limit 50000", sum)
	}
}

func TestGenerateScenario_SoftTenancyMix(t *testing.T) {
	g := newGen(t)
	seq := g.GenerateScenario(generator.ScenarioSoftTenancyMix)
	if len(seq) != 2 {
		t.Fatalf("SOFT_TENANCY_MIX: want 2 txns, got %d", len(seq))
	}
	types := map[model.SourceType]bool{}
	for _, step := range seq {
		types[step.Transaction.SourceType] = true
		if step.Transaction.Amount > 10_000 {
			t.Errorf("SOFT_TENANCY_MIX: amount %f should be under threshold", step.Transaction.Amount)
		}
	}
	if !types[model.SourceTypeBank] || !types[model.SourceTypeMerchant] {
		t.Errorf("SOFT_TENANCY_MIX: want both BANK and MERCHANT, got %v", types)
	}
}

func TestGenerateScenario_MVPSeed(t *testing.T) {
	g := newGen(t)
	seq := g.GenerateScenario(generator.ScenarioMVPSeed)
	if len(seq) != 3 {
		t.Fatalf("MVP_SEED: want 3 txns, got %d", len(seq))
	}
	if seq[0].Transaction.SourceType != model.SourceTypeBank {
		t.Error("MVP_SEED: first txn should be BANK")
	}
	if seq[1].Transaction.SourceType != model.SourceTypeMerchant {
		t.Error("MVP_SEED: second txn should be MERCHANT")
	}
	if seq[2].Transaction.Amount <= 10_000 {
		t.Errorf("MVP_SEED: third txn amount %f must be over threshold", seq[2].Transaction.Amount)
	}
}

func TestGenerateScenario_Unknown_ReturnsNil(t *testing.T) {
	g := newGen(t)
	seq := g.GenerateScenario(generator.ScenarioID("NOPE"))
	if seq != nil {
		t.Fatalf("unknown scenario: want nil, got %d steps", len(seq))
	}
}

func TestGenerateTimedSequence_Normal_UnderAmountThreshold(t *testing.T) {
	g := newGen(t)
	for i := range 40 {
		seq := g.GenerateTimedSequence(generator.ModeNormal, generator.GenerateOptions{})
		if len(seq) != 1 {
			t.Fatalf("trial %d: NORMAL should yield 1 step, got %d", i, len(seq))
		}
		if seq[0].Transaction.Amount < 10 || seq[0].Transaction.Amount > 5_000 {
			t.Errorf("trial %d: normal amount %f out of [10, 5000]", i, seq[0].Transaction.Amount)
		}
	}
}

func TestGenerateTimedSequence_Fraud_EmitsFullVelocitySequence(t *testing.T) {
	g := newGen(t)
	pattern := generator.FraudPatternVelocity
	seq := g.GenerateTimedSequence(generator.ModeFraud, generator.GenerateOptions{Pattern: &pattern})
	if len(seq) < 5 {
		t.Fatalf("FRAUD velocity: want full sequence (>=5), got %d", len(seq))
	}
	for i := 1; i < len(seq); i++ {
		if seq[i].DelayBefore != time.Second {
			t.Errorf("velocity step[%d]: DelayBefore want 1s, got %v", i, seq[i].DelayBefore)
		}
	}
}

func TestGenerateTimedSequence_Fraud_NeverImpossibleTravelWhenRandom(t *testing.T) {
	g := newGen(t)
	for range 80 {
		seq := g.GenerateTimedSequence(generator.ModeFraud, generator.GenerateOptions{})
		if len(seq) == 0 {
			t.Fatal("FRAUD: empty sequence")
		}
		for _, step := range seq {
			desc := ""
			if step.Transaction.Description != nil {
				desc = *step.Transaction.Description
			}
			if strings.Contains(desc, "Impossible travel") {
				t.Fatal("random FRAUD must not pick impossible travel (no matching rule)")
			}
		}
	}
}

func TestGenerateTimedSequence_SourceTypeFilter(t *testing.T) {
	g := newGen(t)
	bank := model.SourceTypeBank
	for i := range 20 {
		seq := g.GenerateTimedSequence(generator.ModeNormal, generator.GenerateOptions{SourceType: &bank})
		if seq[0].Transaction.SourceType != model.SourceTypeBank {
			t.Errorf("trial %d: want BANK, got %s", i, seq[0].Transaction.SourceType)
		}
	}
}

func TestIsValidScenario(t *testing.T) {
	if !generator.IsValidScenario(generator.ScenarioVelocity) {
		t.Error("VELOCITY should be valid")
	}
	if generator.IsValidScenario(generator.ScenarioID("X")) {
		t.Error("X should be invalid")
	}
}
