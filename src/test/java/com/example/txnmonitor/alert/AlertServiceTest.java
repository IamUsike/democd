package com.example.txnmonitor.alert;

import com.example.txnmonitor.api.AlertResponse;
import com.example.txnmonitor.api.PageResponse;
import com.example.txnmonitor.common.exception.AlertNotFoundException;
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
		assertEquals(201L, created.get(0).getAlertId());
		assertEquals("OPEN", created.get(0).getStatus());
		assertEquals("Amount Threshold Rule", created.get(0).getRuleTriggered());
		assertEquals("Triggers when a transaction amount exceeds the configured threshold.",
				created.get(0).getRuleDescription());
		assertEquals("over threshold", created.get(0).getFailingReason());
		assertEquals(1L, created.get(0).getTransactionId());

		ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
		verify(alertRepository).save(alertCaptor.capture());
		assertEquals("BANK", alertCaptor.getValue().getSourceType());
		assertEquals("ACC-1", alertCaptor.getValue().getAccountId());
		assertEquals("Triggers when a transaction amount exceeds the configured threshold.",
				alertCaptor.getValue().getRuleDescription());
		assertEquals("over threshold", alertCaptor.getValue().getFailingReason());

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

		assertEquals("Velocity Rule", created.get(0).getRuleTriggered());
		assertEquals("Triggers when an account performs more than allowed transactions within a specific time window.",
				created.get(0).getRuleDescription());
		assertEquals("too fast", created.get(0).getFailingReason());
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
	void getAlerts_withSourceFilter_returnsPageWithoutTransactionIds() {
		Alert alert = openAlert(1L);
		when(alertRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
				.thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(alert)));

		PageResponse<AlertResponse> results = alertService.getAlerts(
				"BANK", "HSBC-UK", null, null, null, null, null, null, 0, 50, null);

		assertEquals(1, results.items().size());
		assertEquals(1, results.totalCount());
		assertTrue(results.items().get(0).getTransactionIds().isEmpty());
	}

	@Test
	void resolveStatuses_blank_defaultsToActive() {
		assertEquals(
				List.of("OPEN", "ACKNOWLEDGED", "INVESTIGATING"),
				alertService.resolveStatuses(null));
	}

	@Test
	void resolveStatuses_all_meansNoStatusFilter() {
		assertEquals(null, alertService.resolveStatuses("ALL"));
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
		alert.setRuleDescription("Triggers when a transaction amount exceeds the configured threshold.");
		alert.setFailingReason("Amount threshold exceeded.");
		return alert;
	}
}
