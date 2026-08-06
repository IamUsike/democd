package com.example.txnmonitor.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    List<Transaction> findByAccountId(String accountId);

    List<Transaction> findBySourceTypeAndSourceId(String sourceType, String sourceId);

    List<Transaction> findBySourceTypeAndSourceIdAndAccountId(String sourceType, String sourceId, String accountId);

    List<Transaction> findByStatus(String status);

    List<Transaction> findByType(String type);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.accountId = :accountId AND t.timestamp >= :since")
    long countByAccountIdAndTimestampGreaterThanEqual(
            @Param("accountId") String accountId,
            @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.accountId = :accountId AND t.payeeId = :payeeId "
            + "AND t.transactionId <> :excludeTransactionId")
    long countByAccountIdAndPayeeIdAndTransactionIdNot(
            @Param("accountId") String accountId,
            @Param("payeeId") String payeeId,
            @Param("excludeTransactionId") Long excludeTransactionId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.accountId = :accountId "
            + "AND t.type = :type AND t.timestamp >= :start AND t.timestamp < :end")
    BigDecimal sumAmountByAccountIdAndTypeAndTimestampBetween(
            @Param("accountId") String accountId,
            @Param("type") String type,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT t.type, COUNT(t) FROM Transaction t GROUP BY t.type")
    List<Object[]> countGroupedByType();

    @Query("SELECT t.status, COUNT(t) FROM Transaction t GROUP BY t.status")
    List<Object[]> countGroupedByStatus();
}
