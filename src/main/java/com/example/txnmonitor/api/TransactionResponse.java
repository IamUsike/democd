package com.example.txnmonitor.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {

    private Long transactionId;
    private String accountId;
    private String payeeId;
    private BigDecimal amount;
    private String currency;
    private String type;
    private LocalDateTime timestamp;
    private String description;
    private String status;

    public TransactionResponse() {
    }

    public TransactionResponse(Long transactionId, String accountId, String payeeId, BigDecimal amount,
                               String currency, String type, LocalDateTime timestamp, String description,
                               String status) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.payeeId = payeeId;
        this.amount = amount;
        this.currency = currency;
        this.type = type;
        this.timestamp = timestamp;
        this.description = description;
        this.status = status;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
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

