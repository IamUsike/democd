package com.example.txnmonitor.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM transactions");
    }

    @Test
    void findByAccountId_whenTransactionsExist_returnsMatchingRows() {
        insertTransaction("BANK", "HSBC-UK", "HSBC UK", "ACC-1001", "PAYEE-1",
                LocalDateTime.of(2026, 8, 4, 10, 0));
        insertTransaction("MERCHANT", "ACME-POS", "Acme POS", "ACC-2001", "PAYEE-2",
                LocalDateTime.of(2026, 8, 4, 11, 0));

        List<Transaction> transactions = transactionRepository.findByAccountId("ACC-1001");

        assertEquals(1, transactions.size());
        assertEquals("ACC-1001", transactions.get(0).getAccountId());
    }

    @Test
    void findByPayeeId_whenTransactionsExist_returnsMatchingRows() {
        insertTransaction("BANK", "HSBC-UK", "HSBC UK", "ACC-1001", "PAYEE-1",
                LocalDateTime.of(2026, 8, 4, 10, 0));
        insertTransaction("MERCHANT", "ACME-POS", "Acme POS", "ACC-2001", "PAYEE-2",
                LocalDateTime.of(2026, 8, 4, 11, 0));

        List<Transaction> transactions = transactionRepository.findByPayeeId("PAYEE-2");

        assertEquals(1, transactions.size());
        assertEquals("ACC-2001", transactions.get(0).getAccountId());
    }

    @Test
    void findByAccountIdAndPayeeId_whenTransactionsExist_returnsMatchingRows() {
        insertTransaction("BANK", "HSBC-UK", "HSBC UK", "ACC-1001", "PAYEE-1",
                LocalDateTime.of(2026, 8, 4, 10, 0));
        insertTransaction("BANK", "HSBC-UK", "HSBC UK", "ACC-1001", "PAYEE-2",
                LocalDateTime.of(2026, 8, 4, 10, 30));

        List<Transaction> transactions = transactionRepository.findByAccountIdAndPayeeId("ACC-1001", "PAYEE-2");

        assertEquals(1, transactions.size());
        assertEquals("PAYEE-2", transactions.get(0).getPayeeId());
    }

    @Test
    void findBySourceTypeAndSourceId_whenTransactionsExist_returnsMatchingRows() {
        insertTransaction("BANK", "HSBC-UK", "HSBC UK", "ACC-1001", "PAYEE-1",
                LocalDateTime.of(2026, 8, 4, 10, 0));
        insertTransaction("BANK", "BARCLAYS-UK", "Barclays UK", "ACC-1002", "PAYEE-2",
                LocalDateTime.of(2026, 8, 4, 10, 30));

        List<Transaction> transactions = transactionRepository.findBySourceTypeAndSourceId("BANK", "HSBC-UK");

        assertEquals(1, transactions.size());
        assertEquals("ACC-1001", transactions.get(0).getAccountId());
    }

    @Test
    void findByTimestampBetween_whenTransactionsExist_returnsRowsWithinRange() {
        LocalDateTime t1 = LocalDateTime.of(2026, 8, 4, 9, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 8, 4, 10, 0);
        LocalDateTime t3 = LocalDateTime.of(2026, 8, 4, 11, 0);

        insertTransaction("BANK", "HSBC-UK", "HSBC UK", "ACC-1001", "PAYEE-1", t1);
        insertTransaction("BANK", "HSBC-UK", "HSBC UK", "ACC-1002", "PAYEE-2", t2);
        insertTransaction("MERCHANT", "ACME-POS", "Acme POS", "ACC-1003", "PAYEE-3", t3);

        List<Transaction> transactions = transactionRepository.findByTimestampBetween(
                LocalDateTime.of(2026, 8, 4, 9, 30),
                LocalDateTime.of(2026, 8, 4, 10, 30)
        );

        assertEquals(1, transactions.size());
        assertEquals("ACC-1002", transactions.get(0).getAccountId());
    }

    @Test
    void findBySourceTypeAndSourceIdAndTimestampBetween_whenTransactionsExist_returnsRowsWithinRange() {
        insertTransaction("BANK", "HSBC-UK", "HSBC UK", "ACC-1001", "PAYEE-1",
                LocalDateTime.of(2026, 8, 4, 9, 0));
        insertTransaction("BANK", "HSBC-UK", "HSBC UK", "ACC-1002", "PAYEE-2",
                LocalDateTime.of(2026, 8, 4, 10, 0));
        insertTransaction("BANK", "HSBC-UK", "HSBC UK", "ACC-1003", "PAYEE-3",
                LocalDateTime.of(2026, 8, 4, 11, 0));
        insertTransaction("MERCHANT", "ACME-POS", "Acme POS", "ACC-1004", "PAYEE-4",
                LocalDateTime.of(2026, 8, 4, 10, 0));

        List<Transaction> transactions = transactionRepository.findBySourceTypeAndSourceIdAndTimestampBetween(
                "BANK",
                "HSBC-UK",
                LocalDateTime.of(2026, 8, 4, 9, 30),
                LocalDateTime.of(2026, 8, 4, 10, 30)
        );

        assertEquals(1, transactions.size());
        assertEquals("ACC-1002", transactions.get(0).getAccountId());
    }

    private void insertTransaction(String sourceType,
                                   String sourceId,
                                   String sourceName,
                                   String accountId,
                                   String payeeId,
                                   LocalDateTime timestamp) {
        jdbcTemplate.update("""
                        INSERT INTO transactions (
                            source_type, source_id, source_name,
                            account_id, payee_id, payee_name,
                            amount, currency, type,
                            `timestamp`, location, latitude, longitude,
                            description, status
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                sourceType,
                sourceId,
                sourceName,
                accountId,
                payeeId,
                "Payee Name",
                new BigDecimal("100.00"),
                "USD",
                "DEBIT",
                Timestamp.valueOf(timestamp),
                "London",
                new BigDecimal("51.5074000"),
                new BigDecimal("-0.1278000"),
                "Test transaction",
                "COMPLETED"
        );
    }
}

