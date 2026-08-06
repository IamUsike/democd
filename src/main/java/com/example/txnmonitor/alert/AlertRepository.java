package com.example.txnmonitor.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long>, JpaSpecificationExecutor<Alert> {

	List<Alert> findBySourceTypeAndSourceIdOrderByCreatedAtDesc(String sourceType, String sourceId);

	List<Alert> findByStatusOrderByCreatedAtDesc(String status);

	List<Alert> findBySourceTypeAndSourceIdAndStatusOrderByCreatedAtDesc(
			String sourceType,
			String sourceId,
			String status);

	List<Alert> findAllByOrderByCreatedAtDesc();

	long countByStatus(String status);

	long countBySeverity(String severity);

	@Query("SELECT a.status, COUNT(a) FROM Alert a GROUP BY a.status")
	List<Object[]> countGroupedByStatus();

	@Query("SELECT a.severity, COUNT(a) FROM Alert a GROUP BY a.severity")
	List<Object[]> countGroupedBySeverity();

	/** Raw ruleType column values (may be comma-joined for multi-rule alerts). */
	@Query("SELECT a.ruleType FROM Alert a WHERE a.ruleType IS NOT NULL AND a.ruleType <> ''")
	List<String> findAllRuleTypes();
}
