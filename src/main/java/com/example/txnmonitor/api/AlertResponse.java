package com.example.txnmonitor.api;

import java.time.LocalDateTime;
import java.util.List;

public class AlertResponse {

	private Long alertId;
	private Long transactionId;
	private List<Long> transactionIds;
	private String severity;
	private String status;
	private String ruleTriggered;
	private String ruleType;
	private String accountId;
	private String sourceType;
	private String sourceId;
	private String sourceName;
	private LocalDateTime createdAt;
	private LocalDateTime acknowledgedAt;
	private LocalDateTime investigatingAt;
	private LocalDateTime dismissedAt;
	private LocalDateTime closedAt;
	private String resolutionNotes;

	public AlertResponse() {
	}

	public Long getAlertId() {
		return alertId;
	}

	public void setAlertId(Long alertId) {
		this.alertId = alertId;
	}

	public Long getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(Long transactionId) {
		this.transactionId = transactionId;
	}

	public List<Long> getTransactionIds() {
		return transactionIds;
	}

	public void setTransactionIds(List<Long> transactionIds) {
		this.transactionIds = transactionIds;
	}

	public String getSeverity() {
		return severity;
	}

	public void setSeverity(String severity) {
		this.severity = severity;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getRuleTriggered() {
		return ruleTriggered;
	}

	public void setRuleTriggered(String ruleTriggered) {
		this.ruleTriggered = ruleTriggered;
	}

	public String getRuleType() {
		return ruleType;
	}

	public void setRuleType(String ruleType) {
		this.ruleType = ruleType;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public String getSourceType() {
		return sourceType;
	}

	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public String getSourceName() {
		return sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getAcknowledgedAt() {
		return acknowledgedAt;
	}

	public void setAcknowledgedAt(LocalDateTime acknowledgedAt) {
		this.acknowledgedAt = acknowledgedAt;
	}

	public LocalDateTime getInvestigatingAt() {
		return investigatingAt;
	}

	public void setInvestigatingAt(LocalDateTime investigatingAt) {
		this.investigatingAt = investigatingAt;
	}

	public LocalDateTime getDismissedAt() {
		return dismissedAt;
	}

	public void setDismissedAt(LocalDateTime dismissedAt) {
		this.dismissedAt = dismissedAt;
	}

	public LocalDateTime getClosedAt() {
		return closedAt;
	}

	public void setClosedAt(LocalDateTime closedAt) {
		this.closedAt = closedAt;
	}

	public String getResolutionNotes() {
		return resolutionNotes;
	}

	public void setResolutionNotes(String resolutionNotes) {
		this.resolutionNotes = resolutionNotes;
	}
}
