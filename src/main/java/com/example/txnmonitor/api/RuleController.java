package com.example.txnmonitor.api;

import com.example.txnmonitor.common.ApiResponse;
import com.example.txnmonitor.rule.RuleConfigService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rules")
public class RuleController {

	private final RuleConfigService ruleConfigService;

	public RuleController(RuleConfigService ruleConfigService) {
		this.ruleConfigService = ruleConfigService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<RuleConfigResponse>>> getAllRules() {
		List<RuleConfigResponse> rules = ruleConfigService.getAllRules();
		return ResponseEntity.ok(ApiResponse.ok("Rules retrieved successfully.", rules));
	}

	@GetMapping("/{ruleType}")
	public ResponseEntity<ApiResponse<RuleConfigResponse>> getRule(@PathVariable String ruleType) {
		RuleConfigResponse rule = ruleConfigService.getRuleByType(ruleType);
		return ResponseEntity.ok(ApiResponse.ok("Rule retrieved successfully.", rule));
	}

	@PutMapping("/{ruleType}")
	public ResponseEntity<ApiResponse<RuleConfigResponse>> updateRule(
			@PathVariable String ruleType,
			@Valid @RequestBody RuleConfigUpdateRequest request) {
		RuleConfigResponse updated = ruleConfigService.updateRule(ruleType, request);
		return ResponseEntity.ok(ApiResponse.ok("Rule updated successfully.", updated));
	}
}
