package com.example.txnmonitor.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

	List<Alert> findBySourceTypeAndSourceIdOrderByCreatedAtDesc(String sourceType, String sourceId);

	List<Alert> findByStatusOrderByCreatedAtDesc(String status);

	List<Alert> findBySourceTypeAndSourceIdAndStatusOrderByCreatedAtDesc(
			String sourceType,
			String sourceId,
			String status);

	List<Alert> findAllByOrderByCreatedAtDesc();

	long countByStatus(String status);

	long countBySeverity(String severity);

	@Query("""
			SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
			FROM Alert a, AlertTransaction at
			WHERE a.alertId = at.alertId
			  AND at.transactionId = :transactionId
			  AND a.ruleType = :ruleType
			""")
	boolean existsByTransactionIdAndRuleType(
			@Param("transactionId") Long transactionId,
			@Param("ruleType") String ruleType);
}
