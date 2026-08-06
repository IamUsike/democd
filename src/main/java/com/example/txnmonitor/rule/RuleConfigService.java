package com.example.txnmonitor.rule;

import com.example.txnmonitor.api.RuleConfigResponse;
import com.example.txnmonitor.api.RuleConfigUpdateRequest;
import com.example.txnmonitor.common.exception.InvalidRuleConfigException;
import com.example.txnmonitor.common.exception.RuleNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manages rule configuration persistence and propagates runtime changes to live rule beans.
 *
 * <p>Uses the {@link ConfigurableRule} interface to apply config changes without coupling
 * to concrete rule types — adding a new rule type requires no changes here.
 */
@Service
public class RuleConfigService {

	private final RuleConfigRepository ruleConfigRepository;
	/** Lookup map from ruleType string → live ConfigurableRule bean. */
	private final Map<String, ConfigurableRule> rulesByType;

	public RuleConfigService(RuleConfigRepository ruleConfigRepository,
							 List<ConfigurableRule> configurableRules) {
		this.ruleConfigRepository = ruleConfigRepository;
		this.rulesByType = configurableRules.stream()
				.collect(Collectors.toMap(ConfigurableRule::ruleType, Function.identity()));
	}

	@Transactional(readOnly = true)
	public List<RuleConfigResponse> getAllRules() {
		return ruleConfigRepository.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public RuleConfigResponse getRuleByType(String ruleType) {
		RuleConfig config = ruleConfigRepository.findById(ruleType.toUpperCase())
				.orElseThrow(() -> new RuleNotFoundException(ruleType));
		return toResponse(config);
	}

	@Transactional
	public RuleConfigResponse updateRule(String ruleType, RuleConfigUpdateRequest request) {
		String key = ruleType.toUpperCase();
		RuleConfig config = ruleConfigRepository.findById(key)
				.orElseThrow(() -> new RuleNotFoundException(ruleType));

		rejectForeignParameters(key, request);

		if (request.getEnabled() != null) {
			config.setEnabled(request.getEnabled());
		}

		switch (key) {
			case "AMOUNT_THRESHOLD" -> {
				if (request.getAmountThreshold() != null) {
					config.setAmountThreshold(request.getAmountThreshold());
				}
			}
			case "VELOCITY" -> {
				if (request.getVelocityMaxTransactions() != null) {
					config.setVelocityMaxTransactions(request.getVelocityMaxTransactions());
				}
				if (request.getVelocityWindowMinutes() != null) {
					config.setVelocityWindowMinutes(request.getVelocityWindowMinutes());
				}
			}
			case "DAILY_LIMIT" -> {
				if (request.getDailyLimit() != null) {
					config.setDailyLimit(request.getDailyLimit());
				}
			}
			case "NEW_PAYEE" -> {
				// enable/disable only — no numeric params
			}
			default -> {
				// unknown types still only allow enabled (already applied)
			}
		}

		config.setUpdatedAt(LocalDateTime.now());
		RuleConfig saved = ruleConfigRepository.save(config);

		ConfigurableRule liveRule = rulesByType.get(saved.getRuleType());
		if (liveRule != null) {
			liveRule.applyConfig(saved);
		}

		return toResponse(saved);
	}

	/**
	 * Rejects numeric fields that do not belong to the target rule type so the DB
	 * row cannot be polluted by cross-type PUT payloads.
	 */
	private void rejectForeignParameters(String ruleType, RuleConfigUpdateRequest request) {
		switch (ruleType) {
			case "AMOUNT_THRESHOLD" -> {
				rejectIfPresent(ruleType, "velocityMaxTransactions", request.getVelocityMaxTransactions());
				rejectIfPresent(ruleType, "velocityWindowMinutes", request.getVelocityWindowMinutes());
				rejectIfPresent(ruleType, "dailyLimit", request.getDailyLimit());
			}
			case "VELOCITY" -> {
				rejectIfPresent(ruleType, "amountThreshold", request.getAmountThreshold());
				rejectIfPresent(ruleType, "dailyLimit", request.getDailyLimit());
			}
			case "DAILY_LIMIT" -> {
				rejectIfPresent(ruleType, "amountThreshold", request.getAmountThreshold());
				rejectIfPresent(ruleType, "velocityMaxTransactions", request.getVelocityMaxTransactions());
				rejectIfPresent(ruleType, "velocityWindowMinutes", request.getVelocityWindowMinutes());
			}
			case "NEW_PAYEE" -> {
				rejectIfPresent(ruleType, "amountThreshold", request.getAmountThreshold());
				rejectIfPresent(ruleType, "velocityMaxTransactions", request.getVelocityMaxTransactions());
				rejectIfPresent(ruleType, "velocityWindowMinutes", request.getVelocityWindowMinutes());
				rejectIfPresent(ruleType, "dailyLimit", request.getDailyLimit());
			}
			default -> {
				rejectIfPresent(ruleType, "amountThreshold", request.getAmountThreshold());
				rejectIfPresent(ruleType, "velocityMaxTransactions", request.getVelocityMaxTransactions());
				rejectIfPresent(ruleType, "velocityWindowMinutes", request.getVelocityWindowMinutes());
				rejectIfPresent(ruleType, "dailyLimit", request.getDailyLimit());
			}
		}
	}

	private static void rejectIfPresent(String ruleType, String field, Object value) {
		if (value != null) {
			throw new InvalidRuleConfigException(field + " is not valid for rule type " + ruleType);
		}
	}

	private RuleConfigResponse toResponse(RuleConfig config) {
		RuleConfigResponse response = new RuleConfigResponse();
		response.setRuleType(config.getRuleType());
		response.setName(config.getName());
		response.setDescription(config.getDescription());
		response.setEnabled(config.isEnabled());
		response.setAmountThreshold(config.getAmountThreshold());
		response.setVelocityMaxTransactions(config.getVelocityMaxTransactions());
		response.setVelocityWindowMinutes(config.getVelocityWindowMinutes());
		response.setDailyLimit(config.getDailyLimit());
		response.setUpdatedAt(config.getUpdatedAt());
		return response;
	}
}
