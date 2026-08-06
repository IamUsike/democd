package com.example.txnmonitor.rule;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.example.txnmonitor.alert.AlertService;
import com.example.txnmonitor.common.exception.TransactionNotFoundException;
import com.example.txnmonitor.transaction.Transaction;
import com.example.txnmonitor.transaction.TransactionEvaluator;
import com.example.txnmonitor.transaction.TransactionRepository;

@Service
public class TransactionEvaluationService implements TransactionEvaluator {

	private final TransactionRepository transactionRepository;
	private final RuleEngine ruleEngine;
	private final RuleEvaluationContext ruleEvaluationContext;
	private final AlertService alertService;

	public TransactionEvaluationService(
			TransactionRepository transactionRepository,
			RuleEngine ruleEngine,
			RuleEvaluationContext ruleEvaluationContext,
			AlertService alertService) {
		this.transactionRepository = transactionRepository;
		this.ruleEngine = ruleEngine;
		this.ruleEvaluationContext = ruleEvaluationContext;
		this.alertService = alertService;
	}

	public void evaluateByTransactionId(Long transactionId) {
		Objects.requireNonNull(transactionId, "transactionId");
		Transaction transaction = transactionRepository.findById(transactionId)
				.orElseThrow(() -> new TransactionNotFoundException(transactionId));
		evaluate(transaction);
	}

	public void evaluate(Transaction transaction) {
		Objects.requireNonNull(transaction, "transaction");
		List<RuleMatch> matches = ruleEngine.evaluate(
				toSnapshot(transaction),
				ruleEvaluationContext);
		alertService.createFromMatches(transaction, matches);
	}

	private TransactionSnapshot toSnapshot(Transaction transaction) {
		return new TransactionSnapshot(
				transaction.getTransactionId(),
				transaction.getAmount(),
				transaction.getAccountId(),
				transaction.getPayeeId(),
				transaction.getTimestamp(),
				transaction.getType());
	}
}
