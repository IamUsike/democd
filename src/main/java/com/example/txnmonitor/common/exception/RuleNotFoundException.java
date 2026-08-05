package com.example.txnmonitor.common.exception;

public class RuleNotFoundException extends RuntimeException {

	public RuleNotFoundException(String ruleType) {
		super("Rule not found: " + ruleType);
	}
}

