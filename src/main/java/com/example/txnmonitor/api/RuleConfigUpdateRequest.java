package com.example.txnmonitor.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public class RuleConfigUpdateRequest {

	private Boolean enabled;

	@DecimalMin(value = "0.01", message = "amountThreshold must be greater than 0")
	private BigDecimal amountThreshold;

	@Min(value = 1, message = "velocityMaxTransactions must be at least 1")
	private Integer velocityMaxTransactions;

	@Min(value = 1, message = "velocityWindowMinutes must be at least 1")
	private Integer velocityWindowMinutes;

	@DecimalMin(value = "0.01", message = "dailyLimit must be greater than 0")
	private BigDecimal dailyLimit;

	public RuleConfigUpdateRequest() {}

	public Boolean getEnabled() { return enabled; }
	public void setEnabled(Boolean enabled) { this.enabled = enabled; }

	public BigDecimal getAmountThreshold() { return amountThreshold; }
	public void setAmountThreshold(BigDecimal amountThreshold) { this.amountThreshold = amountThreshold; }

	public Integer getVelocityMaxTransactions() { return velocityMaxTransactions; }
	public void setVelocityMaxTransactions(Integer velocityMaxTransactions) { this.velocityMaxTransactions = velocityMaxTransactions; }

	public Integer getVelocityWindowMinutes() { return velocityWindowMinutes; }
	public void setVelocityWindowMinutes(Integer velocityWindowMinutes) { this.velocityWindowMinutes = velocityWindowMinutes; }

	public BigDecimal getDailyLimit() { return dailyLimit; }
	public void setDailyLimit(BigDecimal dailyLimit) { this.dailyLimit = dailyLimit; }
}

