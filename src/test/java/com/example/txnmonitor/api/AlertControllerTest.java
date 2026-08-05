package com.example.txnmonitor.api;

import com.example.txnmonitor.alert.AlertService;
import com.example.txnmonitor.common.GlobalExceptionHandler;
import com.example.txnmonitor.common.exception.AlertNotFoundException;
import com.example.txnmonitor.common.exception.InvalidAlertTransitionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AlertControllerTest {

	private RecordingAlertService alertService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		alertService = new RecordingAlertService();
		ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders.standaloneSetup(new AlertController(alertService))
				.setControllerAdvice(new GlobalExceptionHandler())
				.setValidator(validator)
				.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
				.build();
	}

	@Test
	void getAlertsReturnsOkEnvelope() throws Exception {
		alertService.alertsToReturn = List.of(sampleAlert());

		mockMvc.perform(get("/api/v1/alerts"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data[0].alertId").value(201))
				.andExpect(jsonPath("$.data[0].status").value("OPEN"));

		assertEquals(null, alertService.lastStatusFilter);
	}

	@Test
	void getAlertsWithOpenApiStatusParamPassesStatus() throws Exception {
		alertService.alertsToReturn = List.of(sampleAlert());

		mockMvc.perform(get("/api/v1/alerts").param("status", "OPEN"))
				.andExpect(status().isOk());

		assertEquals("OPEN", alertService.lastStatusFilter);
	}

	@Test
	void getAlertsBackwardCompatiblePathReturnsOkEnvelope() throws Exception {
		alertService.alertsToReturn = List.of(sampleAlert());

		mockMvc.perform(get("/api/alerts"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data[0].alertId").value(201));
	}

	@Test
	void getAlertsWithFiltersPassesParams() throws Exception {
		alertService.alertsToReturn = List.of(sampleAlert());

		mockMvc.perform(get("/api/v1/alerts")
						.param("sourceType", "BANK")
						.param("sourceId", "HSBC-UK")
						.param("status", "OPEN"))
				.andExpect(status().isOk());

		assertEquals("BANK", alertService.lastSourceType);
		assertEquals("HSBC-UK", alertService.lastSourceId);
		assertEquals("OPEN", alertService.lastStatusFilter);
	}

	@Test
	void getAlertsWithInvalidStatusReturnsBadRequest() throws Exception {
		alertService.throwInvalidStatusFilter = true;

		mockMvc.perform(get("/api/v1/alerts").param("status", "NOT_A_STATUS"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid alert status filter: NOT_A_STATUS. Allowed values: OPEN, ACKNOWLEDGED, INVESTIGATING, CLOSED, DISMISSED"));
	}

	@Test
	void getAlertByIdReturnsOkEnvelope() throws Exception {
		alertService.alertToReturn = sampleAlert();

		mockMvc.perform(get("/api/v1/alerts/201"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.alertId").value(201))
				.andExpect(jsonPath("$.data.transactionId").value(101));

		assertEquals(201L, alertService.lastRequestedId);
	}

	@Test
	void getAlertStatusesReturnsOkEnvelope() throws Exception {
		alertService.statusesToReturn = List.of("OPEN", "ACKNOWLEDGED", "INVESTIGATING", "CLOSED", "DISMISSED");

		mockMvc.perform(get("/api/v1/alerts/statuses"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data[0]").value("OPEN"))
				.andExpect(jsonPath("$.data[4]").value("DISMISSED"));
	}

	@Test
	void getAlertByIdReturnsNotFound() throws Exception {
		alertService.throwNotFound = true;

		mockMvc.perform(get("/api/v1/alerts/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Alert with ID 999 not found"));
	}

	@Test
	void updateStatusReturnsOk() throws Exception {
		alertService.alertToReturn = sampleAlert();
		alertService.alertToReturn.setStatus("ACKNOWLEDGED");

		mockMvc.perform(patch("/api/v1/alerts/201/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "ACKNOWLEDGED"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.status").value("ACKNOWLEDGED"));

		assertEquals(201L, alertService.lastUpdatedId);
		assertEquals("ACKNOWLEDGED", alertService.lastUpdatedStatus);
	}

	@Test
	void updateStatusReturnsBadRequestForInvalidTransition() throws Exception {
		alertService.throwInvalidTransition = true;

		mockMvc.perform(patch("/api/v1/alerts/201/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "CLOSED"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid alert transition from OPEN to CLOSED"));
	}

	private AlertResponse sampleAlert() {
		AlertResponse response = new AlertResponse();
		response.setAlertId(201L);
		response.setTransactionId(101L);
		response.setTransactionIds(List.of(101L));
		response.setSeverity("HIGH");
		response.setStatus("OPEN");
		response.setRuleTriggered("Amount Threshold Rule");
		response.setRuleType("AMOUNT_THRESHOLD");
		response.setAccountId("ACC-1");
		response.setSourceType("BANK");
		response.setSourceId("HSBC-UK");
		response.setSourceName("HSBC United Kingdom");
		response.setCreatedAt(LocalDateTime.of(2026, 8, 2, 10, 20));
		return response;
	}

	private static final class RecordingAlertService extends AlertService {

		private List<AlertResponse> alertsToReturn = List.of();
		private AlertResponse alertToReturn;
		private Long lastRequestedId;
		private Long lastUpdatedId;
		private String lastUpdatedStatus;
		private String lastSourceType;
		private String lastSourceId;
		private String lastStatusFilter;
		private List<String> statusesToReturn = List.of();
		private boolean throwNotFound;
		private boolean throwInvalidTransition;
		private boolean throwInvalidStatusFilter;

		private RecordingAlertService() {
			super(null, null);
		}

		@Override
		public List<AlertResponse> getAlerts(String sourceType, String sourceId, String status) {
			if (throwInvalidStatusFilter) {
				throw new com.example.txnmonitor.common.exception.InvalidAlertStatusFilterException(
						status,
						List.of("OPEN", "ACKNOWLEDGED", "INVESTIGATING", "CLOSED", "DISMISSED"));
			}
			this.lastSourceType = sourceType;
			this.lastSourceId = sourceId;
			this.lastStatusFilter = status;
			return alertsToReturn;
		}

		@Override
		public AlertResponse getAlertById(Long alertId) {
			if (throwNotFound) {
				throw new AlertNotFoundException(alertId);
			}
			this.lastRequestedId = alertId;
			return alertToReturn;
		}

		@Override
		public List<String> getAvailableStatuses() {
			return statusesToReturn;
		}

		@Override
		public AlertResponse updateStatus(Long alertId, String requestedStatus, String notes) {
			if (throwInvalidTransition) {
				throw new InvalidAlertTransitionException("OPEN", requestedStatus);
			}
			this.lastUpdatedId = alertId;
			this.lastUpdatedStatus = requestedStatus;
			return alertToReturn;
		}
	}
}
