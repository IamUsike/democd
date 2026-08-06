package com.example.txnmonitor.api;

import java.util.List;

public class DashboardAnalyticsResponse {

	private List<GraphPointResponse> transactionsByType;
	private List<GraphPointResponse> transactionsByStatus;
	private List<GraphPointResponse> alertsByStatus;
	private List<GraphPointResponse> alertsBySeverity;

	public DashboardAnalyticsResponse() {
	}

	public DashboardAnalyticsResponse(
			List<GraphPointResponse> transactionsByType,
			List<GraphPointResponse> transactionsByStatus,
			List<GraphPointResponse> alertsByStatus,
			List<GraphPointResponse> alertsBySeverity) {
		this.transactionsByType = transactionsByType;
		this.transactionsByStatus = transactionsByStatus;
		this.alertsByStatus = alertsByStatus;
		this.alertsBySeverity = alertsBySeverity;
	}

	public List<GraphPointResponse> getTransactionsByType() {
		return transactionsByType;
	}

	public void setTransactionsByType(List<GraphPointResponse> transactionsByType) {
		this.transactionsByType = transactionsByType;
	}

	public List<GraphPointResponse> getTransactionsByStatus() {
		return transactionsByStatus;
	}

	public void setTransactionsByStatus(List<GraphPointResponse> transactionsByStatus) {
		this.transactionsByStatus = transactionsByStatus;
	}

	public List<GraphPointResponse> getAlertsByStatus() {
		return alertsByStatus;
	}

	public void setAlertsByStatus(List<GraphPointResponse> alertsByStatus) {
		this.alertsByStatus = alertsByStatus;
	}

	public List<GraphPointResponse> getAlertsBySeverity() {
		return alertsBySeverity;
	}

	public void setAlertsBySeverity(List<GraphPointResponse> alertsBySeverity) {
		this.alertsBySeverity = alertsBySeverity;
	}
}
