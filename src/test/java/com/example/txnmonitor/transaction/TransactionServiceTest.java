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
    void saveTransaction_shouldMapAllRequestFieldsAndSaveTransaction() {
        TransactionRequest request = sampleRequest();
        Transaction savedTransaction = sampleTransaction(1L);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        TransactionResponse result = transactionService.saveTransaction(request);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction capturedTransaction = transactionCaptor.getValue();

        assertEquals(request.getSourceType(), capturedTransaction.getSourceType());
        assertEquals(request.getSourceId(), capturedTransaction.getSourceId());
        assertEquals(request.getSourceName(), capturedTransaction.getSourceName());
        assertEquals(request.getAccountId(), capturedTransaction.getAccountId());
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
        assertEquals(savedTransaction.getSourceType(), result.getSourceType());
        assertEquals(savedTransaction.getSourceId(), result.getSourceId());
        assertEquals(savedTransaction.getSourceName(), result.getSourceName());
        assertEquals(savedTransaction.getAccountId(), result.getAccountId());
        assertEquals(savedTransaction.getPayeeId(), result.getPayeeId());
        assertEquals(savedTransaction.getPayeeName(), result.getPayeeName());
        assertEquals(savedTransaction.getAmount(), result.getAmount());
        assertEquals(savedTransaction.getCurrency(), result.getCurrency());
        assertEquals(savedTransaction.getType(), result.getType());
        assertEquals(savedTransaction.getTimestamp(), result.getTimestamp());
        assertEquals(savedTransaction.getLocation(), result.getLocation());
        assertEquals(savedTransaction.getLatitude(), result.getLatitude());
        assertEquals(savedTransaction.getLongitude(), result.getLongitude());
        assertEquals(savedTransaction.getDescription(), result.getDescription());
        assertEquals(savedTransaction.getStatus(), result.getStatus());
    }

    @Test
    void getTransactionById_shouldReturnTransactionResponse() {
        Transaction transaction = sampleTransaction(1L);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        TransactionResponse result = transactionService.getTransactionById(1L);

        assertEquals(1L, result.getTransactionId());
        assertEquals("BANK", result.getSourceType());
        assertEquals("HSBC-UK", result.getSourceId());
        assertEquals("HSBC UK", result.getSourceName());
        assertEquals("ACC-1", result.getAccountId());
        assertEquals("PAYEE-1", result.getPayeeId());
        assertEquals("Payee Name", result.getPayeeName());
        assertEquals(new BigDecimal("10.00"), result.getAmount());
        assertEquals("USD", result.getCurrency());
        assertEquals("TRANSFER", result.getType());
        assertEquals(LocalDateTime.of(2026, 8, 3, 10, 15, 30), result.getTimestamp());
        assertEquals("London", result.getLocation());
        assertEquals(new BigDecimal("51.5074000"), result.getLatitude());
        assertEquals(new BigDecimal("-0.1278000"), result.getLongitude());
        assertEquals("test", result.getDescription());
        assertEquals("NEW", result.getStatus());
        verify(transactionRepository).findById(1L);
    }

    @Test
    void getTransactionById_shouldThrowExceptionWhenTransactionDoesNotExist() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.empty());

        TransactionNotFoundException exception = assertThrows(TransactionNotFoundException.class,
                () -> transactionService.getTransactionById(1L));

        assertEquals("Transaction not found with id: 1", exception.getMessage());
        verify(transactionRepository).findById(1L);
    }


    private TransactionRequest sampleRequest() {
        return new TransactionRequest(
                "BANK",
                "HSBC-UK",
                "HSBC UK",
                "ACC-1",
                "PAYEE-1",
                "Payee Name",
                new BigDecimal("10.00"),
                "USD",
                "TRANSFER",
                LocalDateTime.of(2026, 8, 3, 10, 15, 30),
                "London",
                new BigDecimal("51.5074000"),
                new BigDecimal("-0.1278000"),
                "test",
                "NEW"
        );
    }

    private Transaction sampleTransaction(Long transactionId) {
        return new Transaction(
                transactionId,
                "BANK",
                "HSBC-UK",
                "HSBC UK",
                "ACC-1",
                "PAYEE-1",
                "Payee Name",
                new BigDecimal("10.00"),
                "USD",
                "TRANSFER",
                LocalDateTime.of(2026, 8, 3, 10, 15, 30),
                "London",
                new BigDecimal("51.5074000"),
                new BigDecimal("-0.1278000"),
                "test",
                "NEW"
        );
    }
}

