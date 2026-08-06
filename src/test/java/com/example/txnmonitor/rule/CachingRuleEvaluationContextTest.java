package com.example.txnmonitor.rule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CachingRuleEvaluationContextTest {

	@Mock
	private RuleEvaluationContext delegate;

	private CachingRuleEvaluationContext cachingContext;

	@BeforeEach
	void setUp() {
		cachingContext = new CachingRuleEvaluationContext(delegate);
	}

	@Test
	void countRecentTransactions_secondCallUsesCache() {
		LocalDateTime since = LocalDateTime.of(2026, 8, 6, 10, 0);
		when(delegate.countRecentTransactions("ACC-1", since)).thenReturn(3L);

		assertEquals(3L, cachingContext.countRecentTransactions("ACC-1", since));
		assertEquals(3L, cachingContext.countRecentTransactions("ACC-1", since));

		verify(delegate, times(1)).countRecentTransactions("ACC-1", since);
	}

	@Test
	void sumDebitAmountOnDate_secondCallUsesCache() {
		LocalDate date = LocalDate.of(2026, 8, 6);
		when(delegate.sumDebitAmountOnDate("ACC-1", date)).thenReturn(new BigDecimal("1000"));

		assertEquals(new BigDecimal("1000"), cachingContext.sumDebitAmountOnDate("ACC-1", date));
		assertEquals(new BigDecimal("1000"), cachingContext.sumDebitAmountOnDate("ACC-1", date));

		verify(delegate, times(1)).sumDebitAmountOnDate("ACC-1", date);
	}
}
