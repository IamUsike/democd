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
}
