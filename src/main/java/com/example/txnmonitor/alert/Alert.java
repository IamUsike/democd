package com.example.txnmonitor.alert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "alerts")
public class Alert {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "alert_id")
	private Long alertId;

	@Column(name = "rule_type", nullable = false, length = 64)
	private String ruleType;

	@Column(name = "account_id", nullable = false, length = 64)
	private String accountId;

	@Column(name = "source_type", nullable = false, length = 20)
	private String sourceType;

	@Column(name = "source_id", nullable = false, length = 64)
	private String sourceId;

	@Column(name = "source_name", nullable = false, length = 128)
	private String sourceName;

	@Column(name = "status", nullable = false, length = 30)
	private String status;

	@Column(name = "severity", nullable = false, length = 20)
	private String severity;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "acknowledged_at")
	private LocalDateTime acknowledgedAt;

	@Column(name = "investigating_at")
	private LocalDateTime investigatingAt;

	@Column(name = "dismissed_at")
	private LocalDateTime dismissedAt;

	@Column(name = "closed_at")
	private LocalDateTime closedAt;

	@Column(name = "resolution_notes", length = 1000)
	private String resolutionNotes;

	@Column(name = "rule_description", length = 500)
	private String ruleDescription;

	@Column(name = "failing_reason", length = 1000)
	private String failingReason;

	public Alert() {
	}

	public Long getAlertId() {
		return alertId;
	}

	public void setAlertId(Long alertId) {
		this.alertId = alertId;
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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getSeverity() {
		return severity;
	}

	public void setSeverity(String severity) {
		this.severity = severity;
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

	public String getRuleDescription() {
		return ruleDescription;
	}

	public void setRuleDescription(String ruleDescription) {
		this.ruleDescription = ruleDescription;
	}

	public String getFailingReason() {
		return failingReason;
	}

	public void setFailingReason(String failingReason) {
		this.failingReason = failingReason;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Alert alert = (Alert) o;
		return alertId != null && Objects.equals(alertId, alert.alertId);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
