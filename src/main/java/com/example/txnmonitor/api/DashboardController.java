package com.example.txnmonitor.api;

import com.example.txnmonitor.alert.AlertRepository;
import com.example.txnmonitor.common.ApiResponse;
import com.example.txnmonitor.transaction.TransactionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Dashboard KPI aggregations")
public class DashboardController {

	private final TransactionRepository transactionRepository;
	private final AlertRepository alertRepository;

	public DashboardController(
			TransactionRepository transactionRepository,
			AlertRepository alertRepository) {
		this.transactionRepository = transactionRepository;
		this.alertRepository = alertRepository;
	}

	@GetMapping
	@Operation(summary = "Dashboard summary", description = "Returns KPI counts for the operator dashboard.")
	public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {
		DashboardSummaryResponse summary = new DashboardSummaryResponse(
				transactionRepository.count(),
				alertRepository.count(),
				alertRepository.countByStatus("OPEN"),
				alertRepository.countByStatus("CLOSED"),
				alertRepository.countBySeverity("HIGH"));
		return ResponseEntity.ok(ApiResponse.ok("Dashboard summary retrieved successfully.", summary));
	}

	@GetMapping("/analytics")
	@Operation(
			summary = "Dashboard analytics",
			description = "Returns full-table group counts for dashboard graphs (not a page sample).")
	public ResponseEntity<ApiResponse<DashboardAnalyticsResponse>> getAnalytics() {
		DashboardAnalyticsResponse analytics = new DashboardAnalyticsResponse(
				toGraphPoints(transactionRepository.countGroupedByType()),
				toGraphPoints(transactionRepository.countGroupedByStatus()),
				toGraphPoints(alertRepository.countGroupedByStatus()),
				toGraphPoints(alertRepository.countGroupedBySeverity()),
				countByRuleType(alertRepository.findAllRuleTypes()));
		return ResponseEntity.ok(ApiResponse.ok("Dashboard analytics retrieved successfully.", analytics));
	}

	/**
	 * Split comma-joined multi-rule values so each rule type gets a slice credit.
	 * Package-visible for unit tests.
	 */
	static List<GraphPointResponse> countByRuleType(List<String> rawRuleTypes) {
		Map<String, Long> counts = new HashMap<>();
		if (rawRuleTypes != null) {
			for (String raw : rawRuleTypes) {
				if (raw == null || raw.isBlank()) {
					continue;
				}
				for (String part : raw.split(",")) {
					String token = part.trim().toUpperCase(Locale.ROOT);
					if (token.isEmpty()) {
						continue;
					}
					counts.merge(token, 1L, Long::sum);
				}
			}
		}
		return counts.entrySet().stream()
				.map(entry -> new GraphPointResponse(entry.getKey(), entry.getValue()))
				.sorted(Comparator.comparingLong(GraphPointResponse::getValue).reversed()
						.thenComparing(GraphPointResponse::getLabel))
				.toList();
	}

	private static List<GraphPointResponse> toGraphPoints(List<Object[]> rows) {
		return rows.stream()
				.map(row -> {
					Object rawLabel = row[0];
					String label = rawLabel == null || rawLabel.toString().isBlank()
							? "UNKNOWN"
							: rawLabel.toString();
					long value = ((Number) row[1]).longValue();
					return new GraphPointResponse(label, value);
				})
				.sorted(Comparator.comparingLong(GraphPointResponse::getValue).reversed())
				.toList();
	}
}
