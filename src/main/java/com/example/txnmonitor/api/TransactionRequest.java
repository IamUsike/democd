package com.example.txnmonitor.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionRequest {

    @NotBlank(message = "Account ID is required")
    private String accountId;

    @NotBlank(message = "Payee ID is required")
    private String payeeId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be exactly 3 characters")
    private String currency;

    @NotBlank(message = "Transaction type is required")
    private String type;

    @NotNull(message = "Timestamp is required")
    private LocalDateTime timestamp;

    private String description;

    @NotBlank(message = "Status is required")
    private String status;

    public TransactionRequest() {
    }

    public TransactionRequest(String accountId, String payeeId, BigDecimal amount, String currency, String type,
                              LocalDateTime timestamp, String description, String status) {
        this.accountId = accountId;
        this.payeeId = payeeId;
        this.amount = amount;
        this.currency = currency;
        this.type = type;
        this.timestamp = timestamp;
        this.description = description;
        this.status = status;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getPayeeId() {
        return payeeId;
    }

    public void setPayeeId(String payeeId) {
        this.payeeId = payeeId;
    }

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

