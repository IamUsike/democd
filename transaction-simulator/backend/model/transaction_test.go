package model_test

import (
	"encoding/json"
	"testing"
	"time"

	"transaction-simulator/model"
)

// ─── LocalDateTime ────────────────────────────────────────────────────────────

func TestLocalDateTime_MarshalJSON(t *testing.T) {
	ts := model.LocalDateTime{Time: time.Date(2026, 8, 6, 10, 30, 0, 0, time.UTC)}
	data, err := json.Marshal(ts)
	if err != nil {
		t.Fatalf("MarshalJSON error: %v", err)
	}
	want := `"2026-08-06T10:30:00"`
	if string(data) != want {
		t.Errorf("MarshalJSON = %s; want %s", data, want)
	}
}

func TestLocalDateTime_UnmarshalJSON(t *testing.T) {
	input := `"2026-08-06T10:30:00"`
	var ts model.LocalDateTime
	if err := json.Unmarshal([]byte(input), &ts); err != nil {
		t.Fatalf("UnmarshalJSON error: %v", err)
	}
	if ts.Year() != 2026 || ts.Month() != 8 || ts.Day() != 6 {
		t.Errorf("unexpected date: %v", ts.Time)
	}
	if ts.Hour() != 10 || ts.Minute() != 30 || ts.Second() != 0 {
		t.Errorf("unexpected time: %v", ts.Time)
	}
}

func TestLocalDateTime_UnmarshalJSON_InvalidFormat(t *testing.T) {
	var ts model.LocalDateTime
	if err := json.Unmarshal([]byte(`"2026-08-06T10:30:00Z"`), &ts); err == nil {
		t.Error("expected error for timestamp with timezone offset, got nil")
	}
}

// ─── Transaction JSON round-trip ──────────────────────────────────────────────

func TestTransaction_MarshalUnmarshal_AllFields(t *testing.T) {
	payeeName := "Amazon"
	location := "London"
	lat := 51.5074
	lon := -0.1278
	desc := "Online transfer"

	original := model.Transaction{
		SourceType: model.SourceTypeBank,
		SourceID:   "HSBC-UK",
		SourceName: "HSBC United Kingdom",
		AccountID:  "ACC10001",
		PayeeID:    "PAYEE5001",
		PayeeName:  &payeeName,
		Amount:     50000.00,
		Currency:   "USD",
		Type:       model.TransactionTypeTransfer,
		Timestamp:  model.LocalDateTime{Time: time.Date(2026, 8, 6, 10, 30, 0, 0, time.UTC)},
		Location:   &location,
		Latitude:   &lat,
		Longitude:  &lon,
		Description: &desc,
		Status:     model.TransactionStatusCompleted,
	}

	data, err := json.Marshal(original)
	if err != nil {
		t.Fatalf("Marshal error: %v", err)
	}

	var decoded model.Transaction
	if err := json.Unmarshal(data, &decoded); err != nil {
		t.Fatalf("Unmarshal error: %v", err)
	}

	assertEqual(t, "SourceType", string(original.SourceType), string(decoded.SourceType))
	assertEqual(t, "SourceID", original.SourceID, decoded.SourceID)
	assertEqual(t, "SourceName", original.SourceName, decoded.SourceName)
	assertEqual(t, "AccountID", original.AccountID, decoded.AccountID)
	assertEqual(t, "PayeeID", original.PayeeID, decoded.PayeeID)
	assertPtrEqual(t, "PayeeName", original.PayeeName, decoded.PayeeName)
	assertFloat(t, "Amount", original.Amount, decoded.Amount)
	assertEqual(t, "Currency", original.Currency, decoded.Currency)
	assertEqual(t, "Type", string(original.Type), string(decoded.Type))
	assertEqual(t, "Timestamp", "2026-08-06T10:30:00", decoded.Timestamp.Format("2006-01-02T15:04:05"))
	assertPtrEqual(t, "Location", original.Location, decoded.Location)
	assertPtrFloat(t, "Latitude", original.Latitude, decoded.Latitude)
	assertPtrFloat(t, "Longitude", original.Longitude, decoded.Longitude)
	assertPtrEqual(t, "Description", original.Description, decoded.Description)
	assertEqual(t, "Status", string(original.Status), string(decoded.Status))
}

func TestTransaction_OmitsNullableFieldsWhenNil(t *testing.T) {
	txn := model.Transaction{
		SourceType: model.SourceTypeMerchant,
		SourceID:   "ACME-POS",
		SourceName: "Acme Retail",
		AccountID:  "ACC20002",
		PayeeID:    "PAYEE9999",
		Amount:     100.00,
		Currency:   "INR",
		Type:       model.TransactionTypeDebit,
		Timestamp:  model.NowLocal(),
		Status:     model.TransactionStatusFailed,
		// PayeeName, Location, Latitude, Longitude, Description all nil
	}

	data, err := json.Marshal(txn)
	if err != nil {
		t.Fatalf("Marshal error: %v", err)
	}

	var raw map[string]interface{}
	if err := json.Unmarshal(data, &raw); err != nil {
		t.Fatalf("raw Unmarshal error: %v", err)
	}

	for _, field := range []string{"payeeName", "location", "latitude", "longitude", "description"} {
		if _, present := raw[field]; present {
			t.Errorf("field %q should be omitted when nil, but was present in JSON", field)
		}
	}
}

// ─── Enum constant values ─────────────────────────────────────────────────────

func TestEnumValues(t *testing.T) {
	assertEnumStr(t, "SourceTypeBank", "BANK", string(model.SourceTypeBank))
	assertEnumStr(t, "SourceTypeMerchant", "MERCHANT", string(model.SourceTypeMerchant))

	assertEnumStr(t, "TransactionTypeDebit", "DEBIT", string(model.TransactionTypeDebit))
	assertEnumStr(t, "TransactionTypeCredit", "CREDIT", string(model.TransactionTypeCredit))
	assertEnumStr(t, "TransactionTypeTransfer", "TRANSFER", string(model.TransactionTypeTransfer))

	assertEnumStr(t, "TransactionStatusCompleted", "COMPLETED", string(model.TransactionStatusCompleted))
	assertEnumStr(t, "TransactionStatusFailed", "FAILED", string(model.TransactionStatusFailed))
}

// ─── helpers ──────────────────────────────────────────────────────────────────

func assertEqual(t *testing.T, field, want, got string) {
	t.Helper()
	if want != got {
		t.Errorf("%s: want %q, got %q", field, want, got)
	}
}

func assertPtrEqual(t *testing.T, field string, want, got *string) {
	t.Helper()
	if (want == nil) != (got == nil) {
		t.Errorf("%s: nil mismatch — want %v, got %v", field, want, got)
		return
	}
	if want != nil && *want != *got {
		t.Errorf("%s: want %q, got %q", field, *want, *got)
	}
}

func assertFloat(t *testing.T, field string, want, got float64) {
	t.Helper()
	if want != got {
		t.Errorf("%s: want %f, got %f", field, want, got)
	}
}

func assertPtrFloat(t *testing.T, field string, want, got *float64) {
	t.Helper()
	if (want == nil) != (got == nil) {
		t.Errorf("%s: nil mismatch", field)
		return
	}
	if want != nil && *want != *got {
		t.Errorf("%s: want %f, got %f", field, *want, *got)
	}
}

func assertEnumStr(t *testing.T, name, want, got string) {
	t.Helper()
	if want != got {
		t.Errorf("enum %s: want %q, got %q", name, want, got)
	}
}

