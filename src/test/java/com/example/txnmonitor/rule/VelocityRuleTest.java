package com.example.txnmonitor.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VelocityRuleTest {

	private static final int MAX_TRANSACTIONS = 5;
	private static final int WINDOW_MINUTES = 10;
	private static final Long TXN_ID = 42L;
	private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 8, 5, 12, 0);

	@Mock
	private RuleEvaluationContext context;

	private VelocityRule rule;

	@BeforeEach
	void setUp() {
		rule = new VelocityRule(MAX_TRANSACTIONS, WINDOW_MINUTES);
	}

	@Test
	void evaluate_countAboveMax_returnsMatch() {
		TransactionSnapshot txn = snapshot(new BigDecimal("100.00"));
		LocalDateTime since = TIMESTAMP.minusMinutes(WINDOW_MINUTES);
		when(context.countRecentTransactions(eq("ACC-1"), eq(since))).thenReturn(6L);

		List<RuleMatch> matches = rule.evaluate(txn, context);

		assertEquals(1, matches.size());
		RuleMatch match = matches.get(0);
		assertEquals("VELOCITY", match.ruleType());
		assertEquals("LOW", match.severity());
		assertEquals(TXN_ID, match.transactionId());
		assertTrue(match.reason().contains("6"));
		verify(context).countRecentTransactions("ACC-1", since);
	}

	@Test
	void evaluate_countAtMax_returnsEmpty() {
		TransactionSnapshot txn = snapshot(new BigDecimal("100.00"));
		LocalDateTime since = TIMESTAMP.minusMinutes(WINDOW_MINUTES);
		when(context.countRecentTransactions(any(), any())).thenReturn(5L);

		List<RuleMatch> matches = rule.evaluate(txn, context);

		assertTrue(matches.isEmpty());
	}

	@Test
	void evaluate_countBelowMax_returnsEmpty() {
		TransactionSnapshot txn = snapshot(new BigDecimal("100.00"));
		when(context.countRecentTransactions(any(), any())).thenReturn(3L);

		List<RuleMatch> matches = rule.evaluate(txn, context);

		assertTrue(matches.isEmpty());
	}

	private static TransactionSnapshot snapshot(BigDecimal amount) {
		return new TransactionSnapshot(TXN_ID, amount, "ACC-1", "PAYEE-1", TIMESTAMP, "DEBIT");
	}
}
