package com.example.txnmonitor.transaction;

import com.example.txnmonitor.api.TransactionRequest;
import com.example.txnmonitor.api.TransactionResponse;
import com.example.txnmonitor.common.exception.TransactionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactionRepository);
    }

    @Test
    void saveTransactionDelegatesToRepository() {
        TransactionRequest request = new TransactionRequest(
                "BANK",
                "HSBC-UK",
                "HSBC United Kingdom",
                "ACC-1",
                "PAYEE-1",
                "Acme Vendors Ltd",
                new BigDecimal("10.00"),
                "USD",
                "TRANSFER",
                LocalDateTime.of(2026, 8, 3, 10, 15, 30),
                "London, UK",
                new BigDecimal("51.5074000"),
                new BigDecimal("-0.1278000"),
                "test",
                "NEW"
        );
        Transaction savedTransaction = new Transaction(
                1L,
                "BANK",
                "HSBC-UK",
                "HSBC United Kingdom",
                "ACC-1",
                "PAYEE-1",
                "Acme Vendors Ltd",
                new BigDecimal("10.00"),
                "USD",
                "TRANSFER",
                LocalDateTime.of(2026, 8, 3, 10, 15, 30),
                "London, UK",
                new BigDecimal("51.5074000"),
                new BigDecimal("-0.1278000"),
                "test",
                "NEW"
        );
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        TransactionResponse result = transactionService.saveTransaction(request);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction capturedTransaction = transactionCaptor.getValue();

        assertEquals(request.getAccountId(), capturedTransaction.getAccountId());
        assertEquals(request.getSourceType(), capturedTransaction.getSourceType());
        assertEquals(request.getSourceId(), capturedTransaction.getSourceId());
        assertEquals(request.getSourceName(), capturedTransaction.getSourceName());
        assertEquals(request.getPayeeId(), capturedTransaction.getPayeeId());
        assertEquals(request.getPayeeName(), capturedTransaction.getPayeeName());
        assertEquals(request.getAmount(), capturedTransaction.getAmount());
        assertEquals(request.getCurrency(), capturedTransaction.getCurrency());
        assertEquals(request.getType(), capturedTransaction.getType());
        assertEquals(request.getTimestamp(), capturedTransaction.getTimestamp());
        assertEquals(request.getLocation(), capturedTransaction.getLocation());
        assertEquals(request.getLatitude(), capturedTransaction.getLatitude());
        assertEquals(request.getLongitude(), capturedTransaction.getLongitude());
        assertEquals(request.getDescription(), capturedTransaction.getDescription());
        assertEquals(request.getStatus(), capturedTransaction.getStatus());

        assertEquals(1L, result.getTransactionId());
        assertEquals("BANK", result.getSourceType());
        assertEquals("HSBC-UK", result.getSourceId());
        assertEquals("ACC-1", result.getAccountId());
    }

    @Test
    void getAllTransactionsDelegatesToRepository() {
        List<Transaction> transactions = List.of(new Transaction());
        when(transactionRepository.findAll()).thenReturn(transactions);

        List<TransactionResponse> result = transactionService.getAllTransactions();

        assertEquals(1, result.size());
        verify(transactionRepository).findAll();
    }

    @Test
    void getTransactionByIdReturnsTransactionWhenFound() {
        Transaction transaction = new Transaction(
                1L,
                "BANK",
                "HSBC-UK",
                "HSBC United Kingdom",
                "ACC-1",
                "PAYEE-1",
                "Acme Vendors Ltd",
                new BigDecimal("10.00"),
                "USD",
                "TRANSFER",
                LocalDateTime.of(2026, 8, 3, 10, 15, 30),
                "London, UK",
                new BigDecimal("51.5074000"),
                new BigDecimal("-0.1278000"),
                "test",
                "NEW"
        );
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        TransactionResponse result = transactionService.getTransactionById(1L);

        assertEquals(1L, result.getTransactionId());
        verify(transactionRepository).findById(1L);
    }

    @Test
    void getTransactionByIdThrowsWhenMissing() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.empty());

        TransactionNotFoundException exception = assertThrows(TransactionNotFoundException.class,
                () -> transactionService.getTransactionById(1L));

        assertEquals("Transaction with ID 1 not found", exception.getMessage());
        verify(transactionRepository).findById(1L);
    }

    @Test
    void searchMethodsDelegateToRepository() {
        List<Transaction> transactions = List.of(new Transaction());
        when(transactionRepository.findByAccountId("ACC-1")).thenReturn(transactions);
        when(transactionRepository.findByStatus("NEW")).thenReturn(transactions);
        when(transactionRepository.findByType("TRANSFER")).thenReturn(transactions);

        assertEquals(1, transactionService.searchByAccountId("ACC-1").size());
        assertEquals(1, transactionService.searchByStatus("NEW").size());
        assertEquals(1, transactionService.searchByType("TRANSFER").size());

        verify(transactionRepository).findByAccountId("ACC-1");
        verify(transactionRepository).findByStatus("NEW");
        verify(transactionRepository).findByType("TRANSFER");
    }
}

