package com.example.txnmonitor.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountId(String accountId);

    List<Transaction> findByStatus(String status);

    List<Transaction> findByType(String type);
}

