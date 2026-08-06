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
				.andExpect(jsonPath("$.data.alertsBySeverity[2].label").value("HIGH"));
	}
}
