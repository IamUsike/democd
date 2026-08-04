package com.example.txnmonitor.api;

import jakarta.validation.constraints.NotBlank;

public class AlertStatusUpdateRequest {

	@NotBlank(message = "Status is required")
	private String status;

	private String notes;

	public AlertStatusUpdateRequest() {
	}

	public AlertStatusUpdateRequest(String status, String notes) {
		this.status = status;
		this.notes = notes;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}
}
