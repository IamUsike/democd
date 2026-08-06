package com.example.txnmonitor.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertTransactionRepository extends JpaRepository<AlertTransaction, AlertTransaction.AlertTransactionId> {

	List<AlertTransaction> findByAlertId(Long alertId);

	List<AlertTransaction> findByAlertIdIn(List<Long> alertIds);

	boolean existsByTransactionId(Long transactionId);
}
