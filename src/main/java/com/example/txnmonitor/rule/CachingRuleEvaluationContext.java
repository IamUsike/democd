package com.example.txnmonitor.rule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Decorator that caches velocity, new-payee, and daily-limit lookups.
 */
public final class CachingRuleEvaluationContext implements RuleEvaluationContext {

	private final RuleEvaluationContext delegate;
	private final Cache<String, Long> countCache;
	private final Cache<String, BigDecimal> sumCache;

	public CachingRuleEvaluationContext(RuleEvaluationContext delegate) {
		this.delegate = Objects.requireNonNull(delegate, "delegate");
		this.countCache = Caffeine.newBuilder()
				.maximumSize(10_000)
				.expireAfterWrite(10, TimeUnit.MINUTES)
				.build();
		this.sumCache = Caffeine.newBuilder()
				.maximumSize(10_000)
				.expireAfterWrite(1, TimeUnit.DAYS)
				.build();
	}

	@Override
	public long countRecentTransactions(String accountId, LocalDateTime since) {
		String key = "velocity:" + accountId + ":" + since;
		Long cached = countCache.getIfPresent(key);
		if (cached != null) {
			return cached;
		}
		long count = delegate.countRecentTransactions(accountId, since);
		countCache.put(key, count);
		return count;
	}

	@Override
	public long countPriorPayeeTransactions(String accountId, String payeeId, Long excludeTransactionId) {
		String key = "payee:" + accountId + ":" + payeeId + ":" + excludeTransactionId;
		Long cached = countCache.getIfPresent(key);
		if (cached != null) {
			return cached;
		}
		long count = delegate.countPriorPayeeTransactions(accountId, payeeId, excludeTransactionId);
		countCache.put(key, count);
		return count;
	}

	@Override
	public BigDecimal sumDebitAmountOnDate(String accountId, LocalDate date) {
		String key = "daily:" + accountId + ":" + date;
		BigDecimal cached = sumCache.getIfPresent(key);
		if (cached != null) {
			return cached;
		}
		BigDecimal sum = delegate.sumDebitAmountOnDate(accountId, date);
		sumCache.put(key, sum);
		return sum;
	}
}
