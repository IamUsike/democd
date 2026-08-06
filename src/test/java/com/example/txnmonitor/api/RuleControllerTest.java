package com.example.txnmonitor.api;

import com.example.txnmonitor.common.GlobalExceptionHandler;
import com.example.txnmonitor.common.exception.InvalidRuleConfigException;
import com.example.txnmonitor.common.exception.RuleNotFoundException;
import com.example.txnmonitor.rule.RuleConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RuleControllerTest {

	private RecordingRuleConfigService ruleConfigService;
	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		ruleConfigService = new RecordingRuleConfigService();
		objectMapper = new ObjectMapper().findAndRegisterModules();
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders.standaloneSetup(new RuleController(ruleConfigService))
				.setControllerAdvice(new GlobalExceptionHandler())
				.setValidator(validator)
				.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
				.build();
	}

	@Test
	void getAllRules_returnsOkEnvelope() throws Exception {
		ruleConfigService.rulesToReturn = List.of(sampleRule());

		mockMvc.perform(get("/api/v1/rules"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data[0].ruleType").value("AMOUNT_THRESHOLD"))
				.andExpect(jsonPath("$.data[0].enabled").value(true));
	}

	@Test
	void getRule_notFound_returns404() throws Exception {
		ruleConfigService.throwNotFound = true;

		mockMvc.perform(get("/api/v1/rules/NOPE"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Rule not found: NOPE"));
	}

	@Test
	void updateRule_returnsUpdated() throws Exception {
		ruleConfigService.ruleToReturn = sampleRule();
		ruleConfigService.ruleToReturn.setAmountThreshold(new BigDecimal("25000"));

		RuleConfigUpdateRequest body = new RuleConfigUpdateRequest();
		body.setAmountThreshold(new BigDecimal("25000"));

		mockMvc.perform(put("/api/v1/rules/AMOUNT_THRESHOLD")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(body)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.amountThreshold").value(25000));

		assertEquals("AMOUNT_THRESHOLD", ruleConfigService.lastUpdatedType);
	}

	@Test
	void updateRule_invalidConfig_returns400() throws Exception {
		ruleConfigService.throwInvalidConfig = true;

		RuleConfigUpdateRequest body = new RuleConfigUpdateRequest();
		body.setAmountThreshold(new BigDecimal("1"));

		mockMvc.perform(put("/api/v1/rules/VELOCITY")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(body)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("amountThreshold is not valid for rule type VELOCITY"));
	}

	private static RuleConfigResponse sampleRule() {
		RuleConfigResponse response = new RuleConfigResponse();
		response.setRuleType("AMOUNT_THRESHOLD");
		response.setName("Amount Threshold");
		response.setDescription("desc");
		response.setEnabled(true);
		response.setAmountThreshold(new BigDecimal("10000"));
		response.setUpdatedAt(LocalDateTime.of(2026, 8, 6, 10, 0));
		return response;
	}

	private static final class RecordingRuleConfigService extends RuleConfigService {

		private List<RuleConfigResponse> rulesToReturn = new ArrayList<>();
		private RuleConfigResponse ruleToReturn;
		private String lastUpdatedType;
		private boolean throwNotFound;
		private boolean throwInvalidConfig;

		private RecordingRuleConfigService() {
			super(null, List.of());
		}

		@Override
		public List<RuleConfigResponse> getAllRules() {
			return rulesToReturn;
		}

		@Override
		public RuleConfigResponse getRuleByType(String ruleType) {
			if (throwNotFound) {
				throw new RuleNotFoundException(ruleType);
			}
			return ruleToReturn;
		}

		@Override
		public RuleConfigResponse updateRule(String ruleType, RuleConfigUpdateRequest request) {
			lastUpdatedType = ruleType;
			if (throwNotFound) {
				throw new RuleNotFoundException(ruleType);
			}
			if (throwInvalidConfig) {
				throw new InvalidRuleConfigException(
						"amountThreshold is not valid for rule type " + ruleType.toUpperCase());
			}
			return ruleToReturn;
		}
	}
}
