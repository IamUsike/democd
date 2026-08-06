package com.example.txnmonitor.rule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rule_configs")
public class RuleConfig {

	@Id
	@Column(name = "rule_type", length = 64)
	private String ruleType;

	@Column(name = "name", nullable = false, length = 128)
	private String name;

	@Column(name = "description", nullable = false, length = 512)
	private String description;

	@Column(name = "enabled", nullable = false)
	private boolean enabled;

	@Column(name = "amount_threshold", precision = 15, scale = 2)
	private BigDecimal amountThreshold;

	@Column(name = "velocity_max_transactions")
	private Integer velocityMaxTransactions;

	@Column(name = "velocity_window_minutes")
	private Integer velocityWindowMinutes;

	@Column(name = "daily_limit", precision = 15, scale = 2)
	private BigDecimal dailyLimit;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected RuleConfig() {
	}

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

