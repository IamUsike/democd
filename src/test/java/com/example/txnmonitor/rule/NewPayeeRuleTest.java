package com.example.txnmonitor.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
class NewPayeeRuleTest {

	private static final Long TXN_ID = 42L;
	private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 8, 5, 12, 0);

	@Mock
	private RuleEvaluationContext context;

	private NewPayeeRule rule;

	@BeforeEach
	void setUp() {
		rule = new NewPayeeRule();
	}

	@Test
	void evaluate_noPriorPayeeTransactions_returnsMatch() {
		TransactionSnapshot txn = snapshot();
		when(context.countPriorPayeeTransactions("ACC-1", "PAYEE-NEW", TXN_ID)).thenReturn(0L);

		List<RuleMatch> matches = rule.evaluate(txn, context);

		assertEquals(1, matches.size());
		RuleMatch match = matches.get(0);
		assertEquals("NEW_PAYEE", match.ruleType());
		assertEquals("MEDIUM", match.severity());
		assertEquals(TXN_ID, match.transactionId());
		assertTrue(match.reason().contains("PAYEE-NEW"));
	}

	@Test
	void evaluate_priorPayeeTransactionsExist_returnsEmpty() {
		TransactionSnapshot txn = snapshot();
		when(context.countPriorPayeeTransactions("ACC-1", "PAYEE-NEW", TXN_ID)).thenReturn(1L);

		List<RuleMatch> matches = rule.evaluate(txn, context);

		assertTrue(matches.isEmpty());
	}

	private static TransactionSnapshot snapshot() {
		return new TransactionSnapshot(
				TXN_ID, new BigDecimal("100.00"), "ACC-1", "PAYEE-NEW", TIMESTAMP, "DEBIT");
	}
}
