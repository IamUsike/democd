package com.example.txnmonitor.alert;

import com.example.txnmonitor.api.AlertResponse;
import com.example.txnmonitor.common.exception.AlertNotFoundException;
import com.example.txnmonitor.common.exception.InvalidAlertStatusFilterException;
import com.example.txnmonitor.common.exception.InvalidAlertTransitionException;
import com.example.txnmonitor.rule.RuleMatch;
import com.example.txnmonitor.transaction.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Arrays;

@Service
public class AlertService {

	private static final Map<AlertStatus, Set<AlertStatus>> ALLOWED_TRANSITIONS = Map.of(
			AlertStatus.OPEN, EnumSet.of(AlertStatus.ACKNOWLEDGED),
			AlertStatus.ACKNOWLEDGED, EnumSet.of(AlertStatus.INVESTIGATING, AlertStatus.DISMISSED),
			AlertStatus.INVESTIGATING, EnumSet.of(AlertStatus.CLOSED, AlertStatus.DISMISSED),
			AlertStatus.CLOSED, EnumSet.noneOf(AlertStatus.class),
			AlertStatus.DISMISSED, EnumSet.noneOf(AlertStatus.class)
	);

	private final AlertRepository alertRepository;
	private final AlertTransactionRepository alertTransactionRepository;

	public AlertService(AlertRepository alertRepository, AlertTransactionRepository alertTransactionRepository) {
		this.alertRepository = alertRepository;
		this.alertTransactionRepository = alertTransactionRepository;
	}

	@Transactional
	public List<AlertResponse> createFromMatches(Transaction transaction, List<RuleMatch> matches) {
		Objects.requireNonNull(transaction, "transaction");
		if (matches == null || matches.isEmpty()) {
			return List.of();
		}

		List<AlertResponse> created = new ArrayList<>();
		LocalDateTime now = LocalDateTime.now();

		for (RuleMatch match : matches) {
			Alert alert = new Alert();
			alert.setRuleType(match.ruleType());
			alert.setSeverity(match.severity());
			alert.setStatus(AlertStatus.OPEN.name());
			alert.setAccountId(transaction.getAccountId());
			alert.setSourceType(transaction.getSourceType());
			alert.setSourceId(transaction.getSourceId());
			alert.setSourceName(transaction.getSourceName());
			alert.setCreatedAt(now);

			Alert saved = alertRepository.save(alert);
			alertTransactionRepository.save(new AlertTransaction(saved.getAlertId(), transaction.getTransactionId()));
			created.add(toResponse(saved, List.of(transaction.getTransactionId())));
		}

		return created;
	}

	public List<AlertResponse> getAlerts(String sourceType, String sourceId, String status) {
		List<Alert> alerts;
		boolean hasSource = hasText(sourceType) && hasText(sourceId);
		String normalizedStatus = normalizeStatusFilter(status);
		boolean hasStatus = normalizedStatus != null;

		if (hasSource && hasStatus) {
			alerts = alertRepository.findBySourceTypeAndSourceIdAndStatusOrderByCreatedAtDesc(
					sourceType, sourceId, normalizedStatus);
		} else if (hasSource) {
			alerts = alertRepository.findBySourceTypeAndSourceIdOrderByCreatedAtDesc(sourceType, sourceId);
		} else if (hasStatus) {
			alerts = alertRepository.findByStatusOrderByCreatedAtDesc(normalizedStatus);
		} else {
			alerts = alertRepository.findAllByOrderByCreatedAtDesc();
		}

		return toResponses(alerts);
	}

	public List<String> getAvailableStatuses() {
		return Arrays.stream(AlertStatus.values())
				.map(Enum::name)
				.toList();
	}

	public AlertResponse getAlertById(Long alertId) {
		Alert alert = alertRepository.findById(alertId)
				.orElseThrow(() -> new AlertNotFoundException(alertId));
		List<Long> transactionIds = alertTransactionRepository.findByAlertId(alertId).stream()
				.map(AlertTransaction::getTransactionId)
				.toList();
		return toResponse(alert, transactionIds);
	}

	@Transactional
	public AlertResponse updateStatus(Long alertId, String requestedStatus, String notes) {
		Alert alert = alertRepository.findById(alertId)
				.orElseThrow(() -> new AlertNotFoundException(alertId));

		AlertStatus current = AlertStatus.valueOf(alert.getStatus());
		AlertStatus next;
		try {
			next = AlertStatus.valueOf(requestedStatus.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException | NullPointerException ex) {
			throw new InvalidAlertTransitionException(alert.getStatus(), String.valueOf(requestedStatus));
		}

		Set<AlertStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
		if (!allowed.contains(next)) {
			throw new InvalidAlertTransitionException(current.name(), next.name());
		}

		LocalDateTime now = LocalDateTime.now();
		alert.setStatus(next.name());
		switch (next) {
			case ACKNOWLEDGED -> alert.setAcknowledgedAt(now);
			case INVESTIGATING -> alert.setInvestigatingAt(now);
			case CLOSED -> {
				alert.setClosedAt(now);
				if (hasText(notes)) {
					alert.setResolutionNotes(notes);
				}
			}
			case DISMISSED -> {
				alert.setDismissedAt(now);
				if (hasText(notes)) {
					alert.setResolutionNotes(notes);
				}
			}
			default -> {
			}
		}

		Alert saved = alertRepository.save(alert);
		List<Long> transactionIds = alertTransactionRepository.findByAlertId(alertId).stream()
				.map(AlertTransaction::getTransactionId)
				.toList();
		return toResponse(saved, transactionIds);
	}

	private List<AlertResponse> toResponses(List<Alert> alerts) {
		if (alerts.isEmpty()) {
			return List.of();
		}

		List<Long> alertIds = alerts.stream().map(Alert::getAlertId).toList();
		Map<Long, List<Long>> txnIdsByAlert = new HashMap<>();
		for (AlertTransaction link : alertTransactionRepository.findByAlertIdIn(alertIds)) {
			txnIdsByAlert.computeIfAbsent(link.getAlertId(), ignored -> new ArrayList<>())
					.add(link.getTransactionId());
		}

		return alerts.stream()
				.map(alert -> toResponse(alert, txnIdsByAlert.getOrDefault(alert.getAlertId(), List.of())))
				.toList();
	}

	private AlertResponse toResponse(Alert alert, List<Long> transactionIds) {
		AlertResponse response = new AlertResponse();
		response.setAlertId(alert.getAlertId());
		response.setTransactionIds(transactionIds);
		response.setTransactionId(transactionIds.isEmpty() ? null : transactionIds.get(0));
		response.setSeverity(alert.getSeverity());
		response.setStatus(alert.getStatus());
		response.setRuleType(alert.getRuleType());
		response.setRuleTriggered(displayRuleName(alert.getRuleType()));
		response.setAccountId(alert.getAccountId());
		response.setSourceType(alert.getSourceType());
		response.setSourceId(alert.getSourceId());
		response.setSourceName(alert.getSourceName());
		response.setCreatedAt(alert.getCreatedAt());
		response.setAcknowledgedAt(alert.getAcknowledgedAt());
		response.setInvestigatingAt(alert.getInvestigatingAt());
		response.setDismissedAt(alert.getDismissedAt());
		response.setClosedAt(alert.getClosedAt());
		response.setResolutionNotes(alert.getResolutionNotes());
		return response;
	}

	private String displayRuleName(String ruleType) {
		return switch (ruleType) {
			case "AMOUNT_THRESHOLD" -> "Amount Threshold Rule";
			case "VELOCITY" -> "Velocity Rule";
			case "NEW_PAYEE" -> "New Payee Rule";
			case "DAILY_LIMIT" -> "Daily Limit Rule";
			default -> ruleType;
		};
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private String normalizeStatusFilter(String status) {
		if (!hasText(status)) {
			return null;
		}

		String normalized = status.trim().toUpperCase(Locale.ROOT);
		try {
			AlertStatus.valueOf(normalized);
			return normalized;
		} catch (IllegalArgumentException ex) {
			throw new InvalidAlertStatusFilterException(status, getAvailableStatuses());
		}
	}
}
