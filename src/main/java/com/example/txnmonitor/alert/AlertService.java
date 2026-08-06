package com.example.txnmonitor.alert;

import com.example.txnmonitor.api.AlertResponse;
import com.example.txnmonitor.api.PageResponse;
import com.example.txnmonitor.common.PageRequestFactory;
import com.example.txnmonitor.common.exception.AlertNotFoundException;
import com.example.txnmonitor.common.exception.InvalidAlertTransitionException;
import com.example.txnmonitor.rule.RuleMatch;
import com.example.txnmonitor.transaction.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class AlertService {

	private static final Set<String> SORTABLE = Set.of("createdAt", "severity", "status", "alertId");
	private static final List<String> ACTIVE_STATUSES = List.of(
			AlertStatus.OPEN.name(),
			AlertStatus.ACKNOWLEDGED.name(),
			AlertStatus.INVESTIGATING.name());

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

		boolean multipleMatches = matches.size() > 1;

		for (RuleMatch match : matches) {
			Alert alert = new Alert();
			alert.setRuleType(match.ruleType());
			// If multiple rules fire for a single transaction, escalate severity to HIGH
			alert.setSeverity(multipleMatches ? "HIGH" : match.severity());
			alert.setRuleDescription(ruleDescriptionForRuleType(match.ruleType()));
			String reason = resolveFailingReason(match, transaction);
			if (multipleMatches) {
				reason += " [MULTIPLE RULES TRIGGERED: " + matches.size() + " rules matched]";
			}
			alert.setFailingReason(reason);
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

	public PageResponse<AlertResponse> getAlerts(
			String sourceType,
			String sourceId,
			String status,
			String severity,
			String accountId,
			String q,
			LocalDateTime createdFrom,
			LocalDateTime createdTo,
			Integer page,
			Integer size,
			String sort) {
		List<String> statuses = resolveStatuses(status);
		Sort sortSpec = PageRequestFactory.parseSort(sort, "createdAt", SORTABLE);
		Pageable pageable = PageRequestFactory.create(page, size, sortSpec);

		Page<Alert> result = alertRepository.findAll(
				AlertSpecifications.withFilters(
						sourceType, sourceId, statuses, severity, accountId, q, createdFrom, createdTo),
				pageable);

		List<AlertResponse> items = result.getContent().stream()
				.map(alert -> toResponse(alert, List.of()))
				.toList();

		return PageResponse.of(items, result.getTotalElements(), result.getNumber(), result.getSize());
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

	List<String> resolveStatuses(String status) {
		if (!hasText(status)) {
			return ACTIVE_STATUSES;
		}
		if ("ALL".equalsIgnoreCase(status.trim())) {
			return null;
		}
		return List.of(status.trim().toUpperCase(Locale.ROOT));
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
		response.setRuleDescription(alert.getRuleDescription());
		response.setFailingReason(alert.getFailingReason());
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

	private String ruleDescriptionForRuleType(String ruleType) {
		return switch (ruleType) {
			case "AMOUNT_THRESHOLD" -> "Triggers when a transaction amount exceeds the configured threshold.";
			case "VELOCITY" -> "Triggers when an account performs more than allowed transactions within a specific time window.";
			case "NEW_PAYEE" -> "Triggers when a transaction is made to a newly added payee.";
			case "DAILY_LIMIT" -> "Triggers when daily transaction amount exceeds configured limit.";
			default -> "Triggers when rule conditions are satisfied.";
		};
	}

	private String resolveFailingReason(RuleMatch match, Transaction transaction) {
		if (hasText(match.reason())) {
			return match.reason();
		}
		return "Rule " + match.ruleType() + " matched for account " + transaction.getAccountId() + ".";
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
