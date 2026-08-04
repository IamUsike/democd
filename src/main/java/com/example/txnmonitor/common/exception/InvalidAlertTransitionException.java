package com.example.txnmonitor.common.exception;

public class InvalidAlertTransitionException extends RuntimeException {

	public InvalidAlertTransitionException(String currentStatus, String attemptedStatus) {
		super("Invalid alert transition from " + currentStatus + " to " + attemptedStatus);
	}
}
