package com.example.txnmonitor.api;

public class DashboardSummaryResponse {

	private long totalTransactions;
	private long totalAlerts;
	private long openAlerts;
	private long closedAlerts;
	private long highSeverityAlerts;

	public DashboardSummaryResponse() {
	}

	public DashboardSummaryResponse(
			long totalTransactions,
			long totalAlerts,
			long openAlerts,
			long closedAlerts,
			long highSeverityAlerts) {
		this.totalTransactions = totalTransactions;
		this.totalAlerts = totalAlerts;
		this.openAlerts = openAlerts;
		this.closedAlerts = closedAlerts;
		this.highSeverityAlerts = highSeverityAlerts;
	}

	public long getTotalTransactions() {
		return totalTransactions;
	}

	public void setTotalTransactions(long totalTransactions) {
		this.totalTransactions = totalTransactions;
	}

	public long getTotalAlerts() {
		return totalAlerts;
	}

	public void setTotalAlerts(long totalAlerts) {
		this.totalAlerts = totalAlerts;
	}

	public long getOpenAlerts() {
		return openAlerts;
	}

	public void setOpenAlerts(long openAlerts) {
		this.openAlerts = openAlerts;
	}

	public long getClosedAlerts() {
		return closedAlerts;
	}

	public void setClosedAlerts(long closedAlerts) {
		this.closedAlerts = closedAlerts;
	}

	public long getHighSeverityAlerts() {
		return highSeverityAlerts;
	}

	public void setHighSeverityAlerts(long highSeverityAlerts) {
		this.highSeverityAlerts = highSeverityAlerts;
	}
}
