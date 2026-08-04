package com.example.txnmonitor.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountId(String accountId);

    List<Transaction> findByPayeeId(String payeeId);

    List<Transaction> findByAccountIdAndPayeeId(String accountId, String payeeId);

    @Query(value = "SELECT * FROM transactions WHERE source_type = :sourceType", nativeQuery = true)
    List<Transaction> findBySourceType(@Param("sourceType") String sourceType);

    @Query(value = "SELECT * FROM transactions WHERE source_type = :sourceType AND source_id = :sourceId", nativeQuery = true)
    List<Transaction> findBySourceTypeAndSourceId(@Param("sourceType") String sourceType,
                                                   @Param("sourceId") String sourceId);

    @Query(value = "SELECT * FROM transactions WHERE source_type = :sourceType AND source_id = :sourceId " +
            "AND `timestamp` BETWEEN :startTimestamp AND :endTimestamp", nativeQuery = true)
    List<Transaction> findBySourceTypeAndSourceIdAndTimestampBetween(@Param("sourceType") String sourceType,
                                                                     @Param("sourceId") String sourceId,
                                                                     @Param("startTimestamp") LocalDateTime startTimestamp,
                                                                     @Param("endTimestamp") LocalDateTime endTimestamp);

    List<Transaction> findByTimestampBetween(LocalDateTime startTimestamp, LocalDateTime endTimestamp);

    List<Transaction> findByStatus(String status);

    List<Transaction> findByType(String type);
}

