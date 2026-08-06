package com.example.txnmonitor.transaction;

import com.example.txnmonitor.api.TransactionRequest;
import com.example.txnmonitor.api.TransactionResponse;
import com.example.txnmonitor.common.config.TxnMonitorProperties;
import com.example.txnmonitor.common.config.TxnMonitorProperties.Mode;
import com.example.txnmonitor.common.exception.TransactionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private TxnMonitorProperties txnMonitorProperties;
    private RecordingTransactionEvaluator transactionEvaluator;
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        txnMonitorProperties = new TxnMonitorProperties();
        transactionEvaluator = new RecordingTransactionEvaluator();
        transactionService = new TransactionService(
                transactionRepository,
                transactionEvaluator,
                txnMonitorProperties,
                eventPublisher);
    }

    @Test
    void saveTransaction_syncMode_evaluatesInline() {
        txnMonitorProperties.getEvaluation().setMode(Mode.sync);
        TransactionRequest request = sampleRequest();
        Transaction savedTransaction = sampleTransaction();
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        TransactionResponse result = transactionService.saveTransaction(request);

        assertEquals(savedTransaction, transactionEvaluator.lastEvaluatedTransaction);
        verify(eventPublisher, never()).publishEvent(any());
        assertEquals(1L, result.getTransactionId());
    }

    @Test
    void saveTransaction_asyncMode_publishesEventWithoutInlineEvaluation() {
        txnMonitorProperties.getEvaluation().setMode(Mode.async);
        TransactionRequest request = sampleRequest();
        Transaction savedTransaction = sampleTransaction();
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        transactionService.saveTransaction(request);

        assertEquals(0, transactionEvaluator.evaluateCallCount);
        ArgumentCaptor<TransactionEvaluationRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(TransactionEvaluationRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(1L, eventCaptor.getValue().transactionId());
    }

    @Test
    void saveTransactionDelegatesToRepository() {
        txnMonitorProperties.getEvaluation().setMode(Mode.sync);
        TransactionRequest request = sampleRequest();
        Transaction savedTransaction = sampleTransaction();
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        TransactionResponse result = transactionService.saveTransaction(request);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction capturedTransaction = transactionCaptor.getValue();

        assertEquals(request.getAccountId(), capturedTransaction.getAccountId());
        assertEquals(request.getSourceType(), capturedTransaction.getSourceType());
        assertEquals(1L, result.getTransactionId());
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
        Transaction transaction = sampleTransaction();
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
        when(transactionRepository.findBySourceTypeAndSourceId("BANK", "HSBC-UK")).thenReturn(transactions);
        when(transactionRepository.findBySourceTypeAndSourceIdAndAccountId("BANK", "HSBC-UK", "ACC-1"))
                .thenReturn(transactions);
        when(transactionRepository.findByStatus("NEW")).thenReturn(transactions);
        when(transactionRepository.findByType("TRANSFER")).thenReturn(transactions);

        assertEquals(1, transactionService.getTransactions("BANK", "HSBC-UK", null).size());
        assertEquals(1, transactionService.getTransactions("BANK", "HSBC-UK", "ACC-1").size());
        assertEquals(1, transactionService.searchByAccountId("ACC-1").size());
        assertEquals(1, transactionService.searchByStatus("NEW").size());
        assertEquals(1, transactionService.searchByType("TRANSFER").size());
    }

    private TransactionRequest sampleRequest() {
        return new TransactionRequest(
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
                "NEW");
    }

    private Transaction sampleTransaction() {
        return new Transaction(
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
                "NEW");
    }

    private static final class RecordingTransactionEvaluator implements TransactionEvaluator {

        private Transaction lastEvaluatedTransaction;
        private final List<Long> evaluatedTransactionIds = new ArrayList<>();
        private int evaluateCallCount;

        @Override
        public void evaluate(Transaction transaction) {
            evaluateCallCount++;
            lastEvaluatedTransaction = transaction;
        }

        @Override
        public void evaluateByTransactionId(Long transactionId) {
            evaluatedTransactionIds.add(transactionId);
        }
    }
}
