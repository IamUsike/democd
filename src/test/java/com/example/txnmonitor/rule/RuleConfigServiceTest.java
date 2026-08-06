package com.example.txnmonitor.rule;

import com.example.txnmonitor.api.RuleConfigResponse;
import com.example.txnmonitor.api.RuleConfigUpdateRequest;
import com.example.txnmonitor.common.exception.InvalidRuleConfigException;
import com.example.txnmonitor.common.exception.RuleNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleConfigServiceTest {

	@Mock
	private RuleConfigRepository ruleConfigRepository;

	private AmountThresholdRule amountRule;
	private VelocityRule velocityRule;
	private RuleConfigService service;

	@BeforeEach
	void setUp() {
		amountRule = new AmountThresholdRule(new BigDecimal("10000"));
		velocityRule = new VelocityRule(5, 10);
		service = new RuleConfigService(
				ruleConfigRepository,
				List.of(amountRule, velocityRule, new NewPayeeRule(), new DailyLimitRule(new BigDecimal("50000"))));
	}

	@Test
	void updateRule_amountThreshold_updatesOnlyAmountFields() {
		RuleConfig existing = amountConfig(true, new BigDecimal("10000"));
		when(ruleConfigRepository.findById("AMOUNT_THRESHOLD")).thenReturn(Optional.of(existing));
		when(ruleConfigRepository.save(any(RuleConfig.class))).thenAnswer(inv -> inv.getArgument(0));

		RuleConfigUpdateRequest request = new RuleConfigUpdateRequest();
		request.setAmountThreshold(new BigDecimal("25000"));

		RuleConfigResponse response = service.updateRule("AMOUNT_THRESHOLD", request);

		assertEquals(new BigDecimal("25000"), response.getAmountThreshold());
		assertEquals(new BigDecimal("25000"), amountRule.getThreshold());

		ArgumentCaptor<RuleConfig> captor = ArgumentCaptor.forClass(RuleConfig.class);
		verify(ruleConfigRepository).save(captor.capture());
		assertEquals(new BigDecimal("25000"), captor.getValue().getAmountThreshold());
	}

	@Test
	void updateRule_velocityWithAmountThreshold_throwsInvalidRuleConfig() {
		RuleConfig existing = velocityConfig();
		when(ruleConfigRepository.findById("VELOCITY")).thenReturn(Optional.of(existing));

		RuleConfigUpdateRequest request = new RuleConfigUpdateRequest();
		request.setAmountThreshold(new BigDecimal("999"));

		InvalidRuleConfigException ex = assertThrows(
				InvalidRuleConfigException.class,
				() -> service.updateRule("VELOCITY", request));

		assertTrue(ex.getMessage().contains("amountThreshold"));
		assertTrue(ex.getMessage().contains("VELOCITY"));
	}

	@Test
	void updateRule_unknownType_throwsNotFound() {
		when(ruleConfigRepository.findById("NOPE")).thenReturn(Optional.empty());

		assertThrows(RuleNotFoundException.class,
				() -> service.updateRule("NOPE", new RuleConfigUpdateRequest()));
	}

	@Test
	void updateRule_disable_propagatesToLiveBean() {
		RuleConfig existing = amountConfig(true, new BigDecimal("10000"));
		when(ruleConfigRepository.findById("AMOUNT_THRESHOLD")).thenReturn(Optional.of(existing));
		when(ruleConfigRepository.save(any(RuleConfig.class))).thenAnswer(inv -> inv.getArgument(0));

		RuleConfigUpdateRequest request = new RuleConfigUpdateRequest();
		request.setEnabled(false);

		service.updateRule("AMOUNT_THRESHOLD", request);

		assertEquals(false, amountRule.isEnabled());
	}

	private static RuleConfig amountConfig(boolean enabled, BigDecimal threshold) {
		RuleConfig config = new RuleConfig();
		config.setRuleType("AMOUNT_THRESHOLD");
		config.setName("Amount Threshold");
		config.setDescription("desc");
		config.setEnabled(enabled);
		config.setAmountThreshold(threshold);
		return config;
	}

	private static RuleConfig velocityConfig() {
		RuleConfig config = new RuleConfig();
		config.setRuleType("VELOCITY");
		config.setName("Velocity");
		config.setDescription("desc");
		config.setEnabled(true);
		config.setVelocityMaxTransactions(5);
		config.setVelocityWindowMinutes(10);
		return config;
	}
}
