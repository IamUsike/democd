package com.example.txnmonitor.alert;

import com.example.txnmonitor.api.AlertResponse;
import com.example.txnmonitor.common.exception.AlertNotFoundException;
import com.example.txnmonitor.common.exception.InvalidAlertStatusFilterException;
import com.example.txnmonitor.common.exception.InvalidAlertTransitionException;
import com.example.txnmonitor.rule.RuleMatch;
import com.example.txnmonitor.transaction.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

	@Mock
	private AlertRepository alertRepository;

	@Mock
	private AlertTransactionRepository alertTransactionRepository;

	private AlertService alertService;

	@BeforeEach
	void setUp() {
		alertService = new AlertService(alertRepository, alertTransactionRepository);
	}

	@Test
	void createFromMatches_emptyMatches_returnsEmptyAndDoesNotPersist() {
		List<AlertResponse> created = alertService.createFromMatches(sampleTransaction(), List.of());

		assertTrue(created.isEmpty());
		verify(alertRepository, never()).save(any());
	}

	@Test
	void createFromMatches_amountThresholdMatch_persistsAlertAndLink() {
		when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
			Alert alert = invocation.getArgument(0);
			alert.setAlertId(201L);
			return alert;
		});

		List<AlertResponse> created = alertService.createFromMatches(
				sampleTransaction(),
				List.of(new RuleMatch("AMOUNT_THRESHOLD", "HIGH", "over threshold", 1L)));

		assertEquals(1, created.size());
		assertEquals(201L, created.getFirst().getAlertId());
		assertEquals("OPEN", created.getFirst().getStatus());
		assertEquals("Amount Threshold Rule", created.getFirst().getRuleTriggered());
		assertEquals(1L, created.getFirst().getTransactionId());

		ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
		verify(alertRepository).save(alertCaptor.capture());
		assertEquals("BANK", alertCaptor.getValue().getSourceType());
		assertEquals("ACC-1", alertCaptor.getValue().getAccountId());

		verify(alertTransactionRepository).save(any(AlertTransaction.class));
	}

	@Test
	void createFromMatches_velocityMatch_setsDisplayName() {
		when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
			Alert alert = invocation.getArgument(0);
			alert.setAlertId(202L);
			return alert;
		});

		List<AlertResponse> created = alertService.createFromMatches(
				sampleTransaction(),
				List.of(new RuleMatch("VELOCITY", "MEDIUM", "too fast", 1L)));

		assertEquals("Velocity Rule", created.getFirst().getRuleTriggered());
	}

	@Test
	void updateStatus_openToAcknowledged_setsTimestamp() {
		Alert open = openAlert(10L);
		when(alertRepository.findById(10L)).thenReturn(Optional.of(open));
		when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(alertTransactionRepository.findByAlertId(10L))
				.thenReturn(List.of(new AlertTransaction(10L, 1L)));

		AlertResponse updated = alertService.updateStatus(10L, "ACKNOWLEDGED", null);

		assertEquals("ACKNOWLEDGED", updated.getStatus());
		assertNotNull(updated.getAcknowledgedAt());
	}

	@Test
	void updateStatus_closeWithoutAcknowledge_throwsInvalidTransition() {
		Alert open = openAlert(10L);
		when(alertRepository.findById(10L)).thenReturn(Optional.of(open));

		InvalidAlertTransitionException ex = assertThrows(
				InvalidAlertTransitionException.class,
				() -> alertService.updateStatus(10L, "CLOSED", "done"));

		assertTrue(ex.getMessage().contains("OPEN"));
		assertTrue(ex.getMessage().contains("CLOSED"));
	}

	@Test
	void updateStatus_acknowledgedToDismissed_allowed() {
		Alert acknowledged = openAlert(11L);
		acknowledged.setStatus(AlertStatus.ACKNOWLEDGED.name());
		when(alertRepository.findById(11L)).thenReturn(Optional.of(acknowledged));
		when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(alertTransactionRepository.findByAlertId(11L)).thenReturn(List.of());

		AlertResponse updated = alertService.updateStatus(11L, "DISMISSED", "false positive");

		assertEquals("DISMISSED", updated.getStatus());
		assertEquals("false positive", updated.getResolutionNotes());
		assertNotNull(updated.getDismissedAt());
	}

	@Test
	void getAlertById_missing_throwsNotFound() {
		when(alertRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(AlertNotFoundException.class, () -> alertService.getAlertById(999L));
	}

	@Test
	void getAlerts_withSourceFilter_delegatesToRepository() {
		Alert alert = openAlert(1L);
		when(alertRepository.findBySourceTypeAndSourceIdOrderByCreatedAtDesc("BANK", "HSBC-UK"))
				.thenReturn(List.of(alert));
		when(alertTransactionRepository.findByAlertIdIn(List.of(1L)))
				.thenReturn(List.of(new AlertTransaction(1L, 42L)));

		List<AlertResponse> results = alertService.getAlerts("BANK", "HSBC-UK", null);

		assertEquals(1, results.size());
		assertEquals(42L, results.getFirst().getTransactionId());
	}

	@Test
	void getAlerts_withStatusFilter_delegatesToRepository() {
		Alert alert = openAlert(2L);
		alert.setStatus(AlertStatus.CLOSED.name());
		when(alertRepository.findByStatusOrderByCreatedAtDesc("CLOSED"))
				.thenReturn(List.of(alert));
		when(alertTransactionRepository.findByAlertIdIn(List.of(2L)))
				.thenReturn(List.of(new AlertTransaction(2L, 77L)));

		List<AlertResponse> results = alertService.getAlerts(null, null, "closed");

		assertEquals(1, results.size());
		assertEquals("CLOSED", results.getFirst().getStatus());
	}

	@Test
	void getAlerts_withBlankStatus_returnsAllAlerts() {
		Alert alert = openAlert(3L);
		when(alertRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(alert));
		when(alertTransactionRepository.findByAlertIdIn(List.of(3L)))
				.thenReturn(List.of(new AlertTransaction(3L, 88L)));

		List<AlertResponse> results = alertService.getAlerts(null, null, "   ");

		assertEquals(1, results.size());
		assertEquals(88L, results.getFirst().getTransactionId());
	}

	@Test
	void getAlerts_withInvalidStatus_throwsBadRequestException() {
		InvalidAlertStatusFilterException ex = assertThrows(
				InvalidAlertStatusFilterException.class,
				() -> alertService.getAlerts(null, null, "invalid"));

		assertTrue(ex.getMessage().contains("invalid"));
	}

	@Test
	void getAvailableStatuses_returnsAllEnumValuesInDeclarationOrder() {
		List<String> statuses = alertService.getAvailableStatuses();

		assertEquals(List.of("OPEN", "ACKNOWLEDGED", "INVESTIGATING", "CLOSED", "DISMISSED"), statuses);
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

	private Alert openAlert(Long id) {
		Alert alert = new Alert();
		alert.setAlertId(id);
		alert.setRuleType("AMOUNT_THRESHOLD");
		alert.setSeverity("HIGH");
		alert.setStatus(AlertStatus.OPEN.name());
		alert.setAccountId("ACC-1");
		alert.setSourceType("BANK");
		alert.setSourceId("HSBC-UK");
		alert.setSourceName("HSBC United Kingdom");
		alert.setCreatedAt(LocalDateTime.of(2026, 8, 4, 10, 0));
		return alert;
	}
}
