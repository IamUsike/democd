package com.example.txnmonitor.api;

import com.example.txnmonitor.alert.AlertService;
import com.example.txnmonitor.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "Alert retrieval and lifecycle APIs")
public class AlertController {

	private final AlertService alertService;

	public AlertController(AlertService alertService) {
		this.alertService = alertService;
	}

	@GetMapping
	@Operation(summary = "Get alerts", description = "Returns alerts with optional sourceType/sourceId/status filters.")
	public ResponseEntity<ApiResponse<List<AlertResponse>>> getAlerts(
			@RequestParam(required = false) String sourceType,
			@RequestParam(required = false) String sourceId,
			@RequestParam(required = false) String status) {
		List<AlertResponse> alerts = alertService.getAlerts(sourceType, sourceId, status);
		return ResponseEntity.ok(ApiResponse.ok("Alerts retrieved successfully.", alerts));
	}

	@GetMapping("/{alertId}")
	@Operation(summary = "Get alert by ID", description = "Returns alert detail including linked transactions.")
	public ResponseEntity<ApiResponse<AlertResponse>> getAlertById(@PathVariable Long alertId) {
		AlertResponse alert = alertService.getAlertById(alertId);
		return ResponseEntity.ok(ApiResponse.ok("Alert retrieved successfully.", alert));
	}

	@PatchMapping("/{alertId}/status")
	@Operation(summary = "Update alert status", description = "Applies a validated lifecycle transition.")
	public ResponseEntity<ApiResponse<AlertResponse>> updateAlertStatus(
			@PathVariable Long alertId,
			@Valid @RequestBody AlertStatusUpdateRequest request) {
		AlertResponse updated = alertService.updateStatus(alertId, request.getStatus(), request.getNotes());
		return ResponseEntity.ok(ApiResponse.ok("Alert status updated successfully.", updated));
	}
}
