package com.example.txnmonitor.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RuleConfigResponse {

	private String ruleType;
	private String name;
	private String description;
	private boolean enabled;
	private BigDecimal amountThreshold;
	private Integer velocityMaxTransactions;
	private Integer velocityWindowMinutes;
	private BigDecimal dailyLimit;
	private LocalDateTime updatedAt;

	public RuleConfigResponse() {}

	public String getRuleType() { return ruleType; }
	public void setRuleType(String ruleType) { this.ruleType = ruleType; }

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }

	public BigDecimal getAmountThreshold() { return amountThreshold; }
	public void setAmountThreshold(BigDecimal amountThreshold) { this.amountThreshold = amountThreshold; }

	public Integer getVelocityMaxTransactions() { return velocityMaxTransactions; }
	public void setVelocityMaxTransactions(Integer velocityMaxTransactions) { this.velocityMaxTransactions = velocityMaxTransactions; }

	public Integer getVelocityWindowMinutes() { return velocityWindowMinutes; }
	public void setVelocityWindowMinutes(Integer velocityWindowMinutes) { this.velocityWindowMinutes = velocityWindowMinutes; }

	public BigDecimal getDailyLimit() { return dailyLimit; }
	public void setDailyLimit(BigDecimal dailyLimit) { this.dailyLimit = dailyLimit; }

	public LocalDateTime getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

