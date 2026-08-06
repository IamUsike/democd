package com.example.txnmonitor.rule;

import com.example.txnmonitor.alert.AlertService;
import com.example.txnmonitor.api.AlertResponse;
import com.example.txnmonitor.common.exception.TransactionNotFoundException;
import com.example.txnmonitor.rule.RuleMatch;
import com.example.txnmonitor.transaction.Transaction;
import com.example.txnmonitor.transaction.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionEvaluationServiceTest {

	@Mock
	private TransactionRepository transactionRepository;

	private RecordingAlertService alertService;
	private TransactionEvaluationService evaluationService;

	@BeforeEach
	void setUp() {
		alertService = new RecordingAlertService();
		evaluationService = new TransactionEvaluationService(
				transactionRepository,
				new RuleEngine(List.of()),
				new NoOpRuleEvaluationContext(),
				alertService);
	}

	@Test
	void evaluateByTransactionId_missingTransaction_throwsNotFound() {
		when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(TransactionNotFoundException.class, () -> evaluationService.evaluateByTransactionId(99L));
	}

	@Test
	void evaluate_matchingRules_createsAlerts() {
		Transaction transaction = sampleTransaction();
		RuleEngine matchingEngine = new RuleEngine(List.of((txn, context) ->
				List.of(new RuleMatch("AMOUNT_THRESHOLD", "HIGH", "over threshold", 1L))));
		TransactionEvaluationService service = new TransactionEvaluationService(
				transactionRepository,
				matchingEngine,
				new NoOpRuleEvaluationContext(),
				alertService);

		service.evaluate(transaction);

		assertEquals(transaction, alertService.lastTransaction);
		assertEquals(1, alertService.lastMatches.size());
	}

	@Test
	void evaluateByTransactionId_loadsTransactionAndEvaluates() {
		Transaction transaction = sampleTransaction();
		when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

		evaluationService.evaluateByTransactionId(1L);

		assertEquals(transaction, alertService.lastTransaction);
		assertEquals(0, alertService.lastMatches.size());
	}

	private Transaction sampleTransaction() {
		return new Transaction(
				1L,
				"BANK",
				"HSBC-UK",
				"HSBC United Kingdom",
				"ACC-1",
				"PAYEE-1",
				"Acme",
				new BigDecimal("25000"),
				"INR",
				"TRANSFER",
				LocalDateTime.of(2026, 8, 4, 10, 0),
				"London",
				null,
				null,
				"spike",
				"COMPLETED");
	}

	private static final class RecordingAlertService extends AlertService {

		private Transaction lastTransaction;
		private List<RuleMatch> lastMatches = List.of();

		private RecordingAlertService() {
			super(null, null);
		}

		@Override
		public List<AlertResponse> createFromMatches(Transaction transaction, List<RuleMatch> matches) {
			this.lastTransaction = transaction;
			this.lastMatches = new ArrayList<>(matches);
			return List.of();
		}
	}
}
