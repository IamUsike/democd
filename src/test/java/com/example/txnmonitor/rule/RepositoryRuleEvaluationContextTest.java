package com.example.txnmonitor.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.txnmonitor.transaction.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class RepositoryRuleEvaluationContextTest {

	private static final String ACCOUNT_ID = "ACC-1";
	private static final String PAYEE_ID = "PAYEE-1";
	private static final Long TXN_ID = 42L;

	@Mock
	private TransactionRepository transactionRepository;

	private RepositoryRuleEvaluationContext context;

	@BeforeEach
	void setUp() {
		context = new RepositoryRuleEvaluationContext(transactionRepository);
	}

	@Test
	void countRecentTransactions_delegatesToRepository() {
		LocalDateTime since = LocalDateTime.of(2026, 8, 5, 12, 0);
		when(transactionRepository.countByAccountIdAndTimestampGreaterThanEqual(ACCOUNT_ID, since))
				.thenReturn(6L);

		long count = context.countRecentTransactions(ACCOUNT_ID, since);

		assertEquals(6L, count);
		verify(transactionRepository).countByAccountIdAndTimestampGreaterThanEqual(ACCOUNT_ID, since);
	}

	@Test
	void countPriorPayeeTransactions_delegatesToRepository() {
		when(transactionRepository.countByAccountIdAndPayeeIdAndTransactionIdNot(ACCOUNT_ID, PAYEE_ID, TXN_ID))
				.thenReturn(0L);

		long count = context.countPriorPayeeTransactions(ACCOUNT_ID, PAYEE_ID, TXN_ID);

		assertEquals(0L, count);
		verify(transactionRepository).countByAccountIdAndPayeeIdAndTransactionIdNot(ACCOUNT_ID, PAYEE_ID, TXN_ID);
	}

	@Test
	void sumDebitAmountOnDate_delegatesToRepository() {
		LocalDate date = LocalDate.of(2026, 8, 5);
		LocalDateTime start = date.atStartOfDay();
		LocalDateTime end = date.plusDays(1).atStartOfDay();
		when(transactionRepository.sumAmountByAccountIdAndTypeAndTimestampBetween(
				eq(ACCOUNT_ID), eq("DEBIT"), eq(start), eq(end)))
				.thenReturn(new BigDecimal("51000.00"));

		BigDecimal sum = context.sumDebitAmountOnDate(ACCOUNT_ID, date);

		assertEquals(new BigDecimal("51000.00"), sum);
	}

	@Test
	void sumDebitAmountOnDate_nullSumReturnsZero() {
		LocalDate date = LocalDate.of(2026, 8, 5);
		LocalDateTime start = date.atStartOfDay();
		LocalDateTime end = date.plusDays(1).atStartOfDay();
		when(transactionRepository.sumAmountByAccountIdAndTypeAndTimestampBetween(
				eq(ACCOUNT_ID), eq("DEBIT"), eq(start), eq(end)))
				.thenReturn(null);

		BigDecimal sum = context.sumDebitAmountOnDate(ACCOUNT_ID, date);

		assertEquals(BigDecimal.ZERO, sum);
	}
}
