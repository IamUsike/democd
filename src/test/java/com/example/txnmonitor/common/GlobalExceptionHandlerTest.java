package com.example.txnmonitor.common;

import com.example.txnmonitor.common.exception.TransactionNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new TestExceptionController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void handleMethodArgumentNotValidExceptionReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/test-exceptions/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 0,
                                  "currency": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.amount").value("Amount must be greater than zero"))
                .andExpect(jsonPath("$.errors.currency").value("Currency is required"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void handleTransactionNotFoundExceptionReturnsNotFound() throws Exception {
        mockMvc.perform(get("/test-exceptions/not-found/10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Transaction not found with id: 10"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void handleDataIntegrityViolationExceptionReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/test-exceptions/data-integrity"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid transaction data"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void handleUnexpectedExceptionReturnsInternalServerError() throws Exception {
        mockMvc.perform(get("/test-exceptions/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @RestController
    @RequestMapping("/test-exceptions")
    static class TestExceptionController {

        @PostMapping("/validation")
        public String validate(@Valid @RequestBody ValidationRequest request) {
            return "ok";
        }

        @GetMapping("/not-found/{id}")
        public String notFound(@PathVariable Long id) {
            throw new TransactionNotFoundException(id);
        }

        @GetMapping("/data-integrity")
        public String dataIntegrity() {
            throw new DataIntegrityViolationException("Constraint violation");
        }

        @GetMapping("/unexpected")
        public String unexpected() {
            throw new IllegalStateException("Sensitive internal error");
        }
    }

    static class ValidationRequest {

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        private BigDecimal amount;

        @NotBlank(message = "Currency is required")
        private String currency;

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }
}

