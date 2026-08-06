package com.example.txnmonitor.api;

import java.util.List;

import com.example.txnmonitor.rule.RuleMatch;

public record InternalAlertCreateRequest(Long transactionId, List<RuleMatch> matches) {
}
