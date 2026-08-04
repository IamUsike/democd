package com.example.txnmonitor.common.exception;

public class AlertNotFoundException extends RuntimeException {

	public AlertNotFoundException(Long alertId) {
		super("Alert with ID " + alertId + " not found");
	}
}
