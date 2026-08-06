package com.example.txnmonitor.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
}
