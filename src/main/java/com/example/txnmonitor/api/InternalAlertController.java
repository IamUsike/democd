package com.example.txnmonitor.api;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.txnmonitor.alert.AlertService;
import com.example.txnmonitor.common.exception.TransactionNotFoundException;
import com.example.txnmonitor.transaction.Transaction;
import com.example.txnmonitor.transaction.TransactionRepository;

@RestController
@RequestMapping("/internal/alerts")
public class InternalAlertController {

	private final TransactionRepository transactionRepository;
	private final AlertService alertService;

	public InternalAlertController(TransactionRepository transactionRepository, AlertService alertService) {
		this.transactionRepository = transactionRepository;
		this.alertService = alertService;
	}

	@PostMapping
	public List<AlertResponse> createFromMatches(@RequestBody InternalAlertCreateRequest request) {
		Transaction transaction = transactionRepository.findById(request.transactionId())
				.orElseThrow(() -> new TransactionNotFoundException(request.transactionId()));
		return alertService.createFromMatches(transaction, request.matches());
	}
}
