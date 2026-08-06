package com.example.txnmonitor.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyLimitRuleTest {

	private static final BigDecimal DAILY_LIMIT = new BigDecimal("50000");
	private static final Long TXN_ID = 42L;
	private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 8, 5, 14, 30);

	@Mock
	private RuleEvaluationContext context;

	private DailyLimitRule rule;

	@BeforeEach
	void setUp() {
		rule = new DailyLimitRule(DAILY_LIMIT);
	}

	@Test
	void evaluate_debitSumAboveLimit_returnsMatch() {
		TransactionSnapshot txn = snapshot("DEBIT", new BigDecimal("1000.00"));
		when(context.sumDebitAmountOnDate(eq("ACC-1"), eq(LocalDate.of(2026, 8, 5))))
				.thenReturn(new BigDecimal("51000.00"));

		List<RuleMatch> matches = rule.evaluate(txn, context);

		assertEquals(1, matches.size());
		RuleMatch match = matches.get(0);
		assertEquals("DAILY_LIMIT", match.ruleType());
		assertEquals("MEDIUM", match.severity());
		assertEquals(TXN_ID, match.transactionId());
		verify(context).sumDebitAmountOnDate("ACC-1", LocalDate.of(2026, 8, 5));
	}

	@Test
	void evaluate_debitSumAtLimit_returnsEmpty() {
		TransactionSnapshot txn = snapshot("DEBIT", new BigDecimal("1000.00"));
		when(context.sumDebitAmountOnDate(eq("ACC-1"), eq(LocalDate.of(2026, 8, 5))))
				.thenReturn(new BigDecimal("50000.00"));

		List<RuleMatch> matches = rule.evaluate(txn, context);

		assertTrue(matches.isEmpty());
	}

	@Test
	void evaluate_debitSumBelowLimit_returnsEmpty() {
		TransactionSnapshot txn = snapshot("DEBIT", new BigDecimal("1000.00"));
		when(context.sumDebitAmountOnDate(eq("ACC-1"), eq(LocalDate.of(2026, 8, 5))))
				.thenReturn(new BigDecimal("49999.99"));

		List<RuleMatch> matches = rule.evaluate(txn, context);

		assertTrue(matches.isEmpty());
	}

	@Test
	void evaluate_nonDebitTransaction_returnsEmpty() {
		TransactionSnapshot txn = snapshot("CREDIT", new BigDecimal("60000.00"));

		List<RuleMatch> matches = rule.evaluate(txn, context);

		assertTrue(matches.isEmpty());
	}

	private static TransactionSnapshot snapshot(String type, BigDecimal amount) {
		return new TransactionSnapshot(TXN_ID, amount, "ACC-1", "PAYEE-1", TIMESTAMP, type);
	}
}
