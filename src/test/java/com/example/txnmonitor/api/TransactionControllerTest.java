package com.example.txnmonitor.api;

import com.example.txnmonitor.common.GlobalExceptionHandler;
import com.example.txnmonitor.common.exception.TransactionNotFoundException;
import com.example.txnmonitor.transaction.TransactionService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionControllerTest {

    private RecordingTransactionService transactionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        transactionService = new RecordingTransactionService();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new TransactionController(transactionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void createTransactionReturnsCreated() throws Exception {
        transactionService.savedResponse = sampleResponse();

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": "ACC-1",
                                  "payeeId": "PAYEE-1",
                                  "amount": 10.00,
                                  "currency": "USD",
                                  "type": "TRANSFER",
                                  "timestamp": "2026-08-03T10:15:30",
                                  "description": "Test payment",
                                  "status": "NEW"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value(1L))
                .andExpect(jsonPath("$.accountId").value("ACC-1"));

        assertEquals("ACC-1", transactionService.lastSavedRequest.getAccountId());
    }

    @Test
    void getAllTransactionsReturnsOk() throws Exception {
        transactionService.transactionsToReturn = List.of(sampleResponse());

        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionId").value(1L));
    }

    @Test
    void getTransactionByIdReturnsOk() throws Exception {
        transactionService.transactionToReturn = sampleResponse();

        mockMvc.perform(get("/api/v1/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(1L));

        assertEquals(1L, transactionService.lastRequestedId);
    }

    @Test
    void getTransactionsByAccountIdReturnsOk() throws Exception {
        transactionService.transactionsToReturn = List.of(sampleResponse());

        mockMvc.perform(get("/api/v1/transactions/account/ACC-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountId").value("ACC-1"));

        assertEquals("ACC-1", transactionService.lastAccountId);
    }

    @Test
    void getTransactionsByStatusReturnsOk() throws Exception {
        transactionService.transactionsToReturn = List.of(sampleResponse());

        mockMvc.perform(get("/api/v1/transactions/status/NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("NEW"));

        assertEquals("NEW", transactionService.lastStatus);
    }

    @Test
    void getTransactionsByTypeReturnsOk() throws Exception {
        transactionService.transactionsToReturn = List.of(sampleResponse());

        mockMvc.perform(get("/api/v1/transactions/type/TRANSFER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("TRANSFER"));

        assertEquals("TRANSFER", transactionService.lastType);
    }

    @Test
    void getTransactionByIdReturnsNotFoundWhenTransactionNotFoundExceptionOccurs() throws Exception {
        transactionService.throwNotFound = true;

        mockMvc.perform(get("/api/v1/transactions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Transaction with ID 999 not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/transactions/999"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void getTransactionByIdReturnsInternalServerErrorWhenUnexpectedExceptionOccurs() throws Exception {
        transactionService.throwUnexpectedError = true;

        mockMvc.perform(get("/api/v1/transactions/999"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Unexpected failure"))
                .andExpect(jsonPath("$.path").value("/api/v1/transactions/999"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void createTransactionReturnsBadRequestWhenValidationFails() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": "",
                                  "payeeId": "PAYEE-1",
                                  "amount": 10.00,
                                  "currency": "USD",
                                  "type": "TRANSFER",
                                  "timestamp": "2026-08-03T10:15:30",
                                  "description": "Test payment",
                                  "status": "NEW"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Account ID is required"))
                .andExpect(jsonPath("$.path").value("/api/v1/transactions"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    private TransactionResponse sampleResponse() {
        return new TransactionResponse(
                1L,
                "ACC-1",
                "PAYEE-1",
                new BigDecimal("10.00"),
                "USD",
                "TRANSFER",
                LocalDateTime.of(2026, 8, 3, 10, 15, 30),
                "Test payment",
                "NEW"
        );
    }

    private static final class RecordingTransactionService extends TransactionService {

        private TransactionResponse savedResponse;
        private TransactionResponse transactionToReturn;
        private List<TransactionResponse> transactionsToReturn = List.of();
        private TransactionRequest lastSavedRequest;
        private Long lastRequestedId;
        private String lastAccountId;
        private String lastStatus;
        private String lastType;
        private boolean throwNotFound;
        private boolean throwUnexpectedError;

        private RecordingTransactionService() {
            super(null);
        }

        @Override
        public TransactionResponse saveTransaction(TransactionRequest request) {
            this.lastSavedRequest = request;
            return savedResponse;
        }

        @Override
        public List<TransactionResponse> getAllTransactions() {
            return transactionsToReturn;
        }

        @Override
        public TransactionResponse getTransactionById(Long id) {
            if (throwNotFound) {
                throw new TransactionNotFoundException(id);
            }
            if (throwUnexpectedError) {
                throw new IllegalStateException("Unexpected failure");
            }
            this.lastRequestedId = id;
            return transactionToReturn;
        }

        @Override
        public List<TransactionResponse> searchByAccountId(String accountId) {
            this.lastAccountId = accountId;
            return transactionsToReturn;
        }

        @Override
        public List<TransactionResponse> searchByStatus(String status) {
            this.lastStatus = status;
            return transactionsToReturn;
        }

        @Override
        public List<TransactionResponse> searchByType(String type) {
            this.lastType = type;
            return transactionsToReturn;
        }
    }
}

