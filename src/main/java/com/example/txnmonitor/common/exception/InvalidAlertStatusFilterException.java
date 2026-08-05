package com.example.txnmonitor.common.exception;

import java.util.List;

public class InvalidAlertStatusFilterException extends RuntimeException {

	public InvalidAlertStatusFilterException(String status, List<String> allowedStatuses) {
		super("Invalid alert status filter: " + status + ". Allowed values: " + String.join(", ", allowedStatuses));
	}
}

