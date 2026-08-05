package com.example.txnmonitor.rule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import com.example.txnmonitor.transaction.TransactionRepository;

/**
 * Repository-backed lookups for velocity, new payee, and daily limit rules.
 */
public final class RepositoryRuleEvaluationContext implements RuleEvaluationContext {

	private static final String DEBIT_TYPE = "DEBIT";

	private final TransactionRepository transactionRepository;

	public RepositoryRuleEvaluationContext(TransactionRepository transactionRepository) {
		this.transactionRepository = Objects.requireNonNull(transactionRepository, "transactionRepository");
	}

	@Override
	public long countRecentTransactions(String accountId, LocalDateTime since) {
		Objects.requireNonNull(accountId, "accountId");
		Objects.requireNonNull(since, "since");
		return transactionRepository.countByAccountIdAndTimestampGreaterThanEqual(accountId, since);
	}

	@Override
	public long countPriorPayeeTransactions(String accountId, String payeeId, Long excludeTransactionId) {
		Objects.requireNonNull(accountId, "accountId");
		Objects.requireNonNull(payeeId, "payeeId");
		Objects.requireNonNull(excludeTransactionId, "excludeTransactionId");
		return transactionRepository.countByAccountIdAndPayeeIdAndTransactionIdNot(
				accountId, payeeId, excludeTransactionId);
	}

	@Override
	public BigDecimal sumDebitAmountOnDate(String accountId, LocalDate date) {
		Objects.requireNonNull(accountId, "accountId");
		Objects.requireNonNull(date, "date");
		LocalDateTime startOfDay = date.atStartOfDay();
		LocalDateTime startOfNextDay = date.plusDays(1).atStartOfDay();
		BigDecimal sum = transactionRepository.sumAmountByAccountIdAndTypeAndTimestampBetween(
				accountId, DEBIT_TYPE, startOfDay, startOfNextDay);
		return sum != null ? sum : BigDecimal.ZERO;
	}
}
