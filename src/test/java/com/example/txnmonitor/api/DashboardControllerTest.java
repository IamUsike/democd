package com.example.txnmonitor.api;

import com.example.txnmonitor.alert.AlertRepository;
import com.example.txnmonitor.common.GlobalExceptionHandler;
import com.example.txnmonitor.transaction.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardControllerTest {

	private TransactionRepository transactionRepository;
	private AlertRepository alertRepository;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		transactionRepository = mock(TransactionRepository.class);
		alertRepository = mock(AlertRepository.class);
		ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
		mockMvc = MockMvcBuilders.standaloneSetup(new DashboardController(transactionRepository, alertRepository))
				.setControllerAdvice(new GlobalExceptionHandler())
				.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
				.build();
	}

	@Test
	void getSummary_returnsKpiCounts() throws Exception {
		when(transactionRepository.count()).thenReturn(100L);
		when(alertRepository.count()).thenReturn(40L);
		when(alertRepository.countByStatus("OPEN")).thenReturn(25L);
		when(alertRepository.countByStatus("CLOSED")).thenReturn(10L);
		when(alertRepository.countBySeverity("HIGH")).thenReturn(5L);

		mockMvc.perform(get("/api/v1/dashboard"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.totalTransactions").value(100))
				.andExpect(jsonPath("$.data.totalAlerts").value(40))
				.andExpect(jsonPath("$.data.openAlerts").value(25))
				.andExpect(jsonPath("$.data.closedAlerts").value(10))
				.andExpect(jsonPath("$.data.highSeverityAlerts").value(5));
	}

	@Test
	void getAnalytics_returnsGroupedCountsSortedByValueDesc() throws Exception {
		when(transactionRepository.countGroupedByType()).thenReturn(List.of(
				new Object[]{"TRANSFER", 3L},
				new Object[]{"PAYMENT", 10L}));
		when(transactionRepository.countGroupedByStatus()).thenReturn(List.of(
				new Object[]{"COMPLETED", 12L},
				new Object[]{"FAILED", 1L}));
		when(alertRepository.countGroupedByStatus()).thenReturn(List.of(
				new Object[]{"OPEN", 8L},
				new Object[]{"CLOSED", 2L}));
		when(alertRepository.countGroupedBySeverity()).thenReturn(List.of(
				new Object[]{"LOW", 4L},
				new Object[]{"MEDIUM", 5L},
				new Object[]{"HIGH", 1L}));
		when(alertRepository.findAllRuleTypes()).thenReturn(List.of(
				"AMOUNT_THRESHOLD",
				"AMOUNT_THRESHOLD,VELOCITY",
				"NEW_PAYEE",
				"VELOCITY"));

		mockMvc.perform(get("/api/v1/dashboard/analytics"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.transactionsByType[0].label").value("PAYMENT"))
				.andExpect(jsonPath("$.data.transactionsByType[0].value").value(10))
				.andExpect(jsonPath("$.data.transactionsByType[1].label").value("TRANSFER"))
				.andExpect(jsonPath("$.data.transactionsByStatus[0].label").value("COMPLETED"))
				.andExpect(jsonPath("$.data.alertsByStatus[0].label").value("OPEN"))
				.andExpect(jsonPath("$.data.alertsBySeverity[0].label").value("MEDIUM"))
				.andExpect(jsonPath("$.data.alertsBySeverity[0].value").value(5))
				.andExpect(jsonPath("$.data.alertsBySeverity[1].label").value("LOW"))
				.andExpect(jsonPath("$.data.alertsBySeverity[2].label").value("HIGH"))
				.andExpect(jsonPath("$.data.alertsByRuleType[0].label").value("AMOUNT_THRESHOLD"))
				.andExpect(jsonPath("$.data.alertsByRuleType[0].value").value(2))
				.andExpect(jsonPath("$.data.alertsByRuleType[1].label").value("VELOCITY"))
				.andExpect(jsonPath("$.data.alertsByRuleType[1].value").value(2))
				.andExpect(jsonPath("$.data.alertsByRuleType[2].label").value("NEW_PAYEE"))
				.andExpect(jsonPath("$.data.alertsByRuleType[2].value").value(1));
	}

	@Test
	void countByRuleType_splitsCommaJoinedMultiRuleAlerts() {
		List<GraphPointResponse> points = DashboardController.countByRuleType(java.util.Arrays.asList(
				"NEW_PAYEE,VELOCITY",
				"amount_threshold",
				"  ",
				null));

		assertEquals(3, points.size());
		// Same counts → secondary sort by label ascending
		assertEquals("AMOUNT_THRESHOLD", points.get(0).getLabel());
		assertEquals(1L, points.get(0).getValue());
		assertEquals("NEW_PAYEE", points.get(1).getLabel());
		assertEquals(1L, points.get(1).getValue());
		assertEquals("VELOCITY", points.get(2).getLabel());
		assertEquals(1L, points.get(2).getValue());
	}

	@Test
	void countByRuleType_emptyInput_returnsEmptyList() {
		assertEquals(List.of(), DashboardController.countByRuleType(List.of()));
		assertEquals(List.of(), DashboardController.countByRuleType(null));
	}
}
