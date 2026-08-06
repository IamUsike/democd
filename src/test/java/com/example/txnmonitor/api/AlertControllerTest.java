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
	void getAlertsReturnsOkPageEnvelope() throws Exception {
		alertService.pageToReturn = PageResponse.of(List.of(sampleAlert()), 1, 0, 50);

		mockMvc.perform(get("/api/v1/alerts"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items[0].alertId").value(201))
				.andExpect(jsonPath("$.data.items[0].status").value("OPEN"))
				.andExpect(jsonPath("$.data.items[0].ruleDescription").value(
						"Triggers when a transaction amount exceeds the configured threshold."))
				.andExpect(jsonPath("$.data.items[0].failingReason").value(
						"Transaction amount (25000 INR) exceeded threshold (10000 INR)."))
				.andExpect(jsonPath("$.data.totalCount").value(1))
				.andExpect(jsonPath("$.data.page").value(0))
				.andExpect(jsonPath("$.data.size").value(50))
				.andExpect(jsonPath("$.data.hasNext").value(false));
	}

	@Test
	void getAlertsWithFiltersPassesParams() throws Exception {
		alertService.pageToReturn = PageResponse.of(List.of(sampleAlert()), 1, 0, 25);

		mockMvc.perform(get("/api/v1/alerts")
						.param("sourceType", "BANK")
						.param("sourceId", "HSBC-UK")
						.param("status", "OPEN")
						.param("severity", "HIGH")
						.param("accountId", "ACC-1")
						.param("q", "threshold")
						.param("page", "1")
						.param("size", "25")
						.param("sort", "severity,asc"))
				.andExpect(status().isOk());

		assertEquals("BANK", alertService.lastSourceType);
		assertEquals("HSBC-UK", alertService.lastSourceId);
		assertEquals("OPEN", alertService.lastStatusFilter);
		assertEquals("HIGH", alertService.lastSeverity);
		assertEquals("ACC-1", alertService.lastAccountId);
		assertEquals("threshold", alertService.lastQ);
		assertEquals(1, alertService.lastPage);
		assertEquals(25, alertService.lastSize);
		assertEquals("severity,asc", alertService.lastSort);
	}

	@Test
	void getAlertsWithStatusAllPassesThrough() throws Exception {
		alertService.pageToReturn = PageResponse.of(List.of(sampleAlert()), 1, 0, 50);

		mockMvc.perform(get("/api/v1/alerts").param("status", "ALL"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalCount").value(1));

		assertEquals("ALL", alertService.lastStatusFilter);
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
		response.setRuleDescription("Triggers when a transaction amount exceeds the configured threshold.");
		response.setFailingReason("Transaction amount (25000 INR) exceeded threshold (10000 INR).");
		return response;
	}

	private static final class RecordingAlertService extends AlertService {

		private PageResponse<AlertResponse> pageToReturn = PageResponse.of(List.of(), 0, 0, 50);
		private AlertResponse alertToReturn;
		private Long lastRequestedId;
		private Long lastUpdatedId;
		private String lastUpdatedStatus;
		private String lastSourceType;
		private String lastSourceId;
		private String lastStatusFilter;
		private String lastSeverity;
		private String lastAccountId;
		private String lastQ;
		private Integer lastPage;
		private Integer lastSize;
		private String lastSort;
		private boolean throwNotFound;
		private boolean throwInvalidTransition;

		private RecordingAlertService() {
			super(null, null);
		}

		@Override
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
			this.lastSourceType = sourceType;
			this.lastSourceId = sourceId;
			this.lastStatusFilter = status;
			this.lastSeverity = severity;
			this.lastAccountId = accountId;
			this.lastQ = q;
			this.lastPage = page;
			this.lastSize = size;
			this.lastSort = sort;
			return pageToReturn;
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
