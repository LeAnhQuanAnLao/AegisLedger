package com.aegisledger.payment;

import com.aegisledger.account.service.AccountService;
import com.aegisledger.core.domain.Currency;
import com.aegisledger.core.domain.Money;
import com.aegisledger.core.exception.FraudDetectedException;
import com.aegisledger.fraud.domain.FraudCheckContext;
import com.aegisledger.fraud.domain.FraudCheckResult;
import com.aegisledger.fraud.service.FraudEvaluationService;
import com.aegisledger.ledger.service.DoubleEntryLedgerService;
import com.aegisledger.outbox.service.OutboxPublisherService;
import com.aegisledger.payment.domain.SagaStep;
import com.aegisledger.payment.domain.Transaction;
import com.aegisledger.payment.domain.TransactionStatus;
import com.aegisledger.payment.dto.TransferRequest;
import com.aegisledger.payment.dto.TransferResponse;
import com.aegisledger.payment.repository.TransactionRepository;
import com.aegisledger.payment.service.ExternalSwitchService;
import com.aegisledger.payment.service.ExternalSwitchService.SwitchResponse;
import com.aegisledger.payment.service.SagaCoordinator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for SagaCoordinator (Tier 2)")
class SagaCoordinatorTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountService accountService;
    @Mock
    private FraudEvaluationService fraudEvaluationService;
    @Mock
    private ExternalSwitchService externalSwitchService;
    @Mock
    private DoubleEntryLedgerService ledgerService;
    @Mock
    private OutboxPublisherService outboxPublisher;

    @InjectMocks
    private SagaCoordinator sagaCoordinator;

    private UUID sourceId;
    private UUID destId;
    private TransferRequest request;

    @BeforeEach
    void setUp() {
        sourceId = UUID.randomUUID();
        destId = UUID.randomUUID();
        request = new TransferRequest(
            sourceId,
            destId,
            new BigDecimal("250.00"),
            Currency.USD,
            "IDEM-KEY-001",
            "Payment for services"
        );
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Should successfully execute all Saga steps to COMPLETED state")
    void testHappyPathExecution() {
        when(fraudEvaluationService.evaluate(any(FraudCheckContext.class))).thenReturn(FraudCheckResult.pass());
        when(externalSwitchService.dispatchTransfer(any(), eq(sourceId), eq(destId), any(Money.class)))
            .thenReturn(new SwitchResponse(true, "REF-123", null));

        TransferResponse response = sagaCoordinator.executeTransfer(request);

        assertNotNull(response);
        assertEquals(TransactionStatus.COMPLETED, response.status());
        assertEquals(SagaStep.COMMITTED, response.currentStep());

        verify(accountService).holdFunds(eq(sourceId), any(Money.class));
        verify(fraudEvaluationService).evaluate(any(FraudCheckContext.class));
        verify(ledgerService).recordTransfer(any(), eq(sourceId), eq(destId), any(Money.class), eq("Payment for services"), eq(true));
        verify(outboxPublisher).publishEvent(eq("TRANSACTION"), any(), eq("PAYMENT_COMPLETED"), any());
    }

    @Test
    @DisplayName("Should abort and release held funds when fraud screening fails")
    void testFraudRejectedThrowsException() {
        when(fraudEvaluationService.evaluate(any(FraudCheckContext.class)))
            .thenReturn(FraudCheckResult.rejected(85, List.of("Velocity limit exceeded")));

        assertThrows(FraudDetectedException.class, () -> sagaCoordinator.executeTransfer(request));

        verify(accountService).holdFunds(eq(sourceId), any(Money.class));
        verify(accountService).releaseHeldFunds(eq(sourceId), any(Money.class));
        verify(externalSwitchService, never()).dispatchTransfer(any(), any(), any(), any());
        verify(ledgerService, never()).recordTransfer(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("Should trigger Saga Compensation when external switch fails")
    void testExternalSwitchFailureTriggersCompensation() {
        when(fraudEvaluationService.evaluate(any(FraudCheckContext.class))).thenReturn(FraudCheckResult.pass());
        when(externalSwitchService.dispatchTransfer(any(), eq(sourceId), eq(destId), any(Money.class)))
            .thenReturn(new SwitchResponse(false, null, "SWITCH_TIMEOUT: Partner network unreachable"));

        TransferResponse response = sagaCoordinator.executeTransfer(request);

        assertNotNull(response);
        assertEquals(TransactionStatus.COMPLETED.equals(response.status()), false);
        assertEquals(TransactionStatus.COMPENSATED, response.status());
        assertEquals(SagaStep.COMPENSATED, response.currentStep());

        // Verify compensation actions
        verify(accountService).holdFunds(eq(sourceId), any(Money.class));
        verify(accountService).releaseHeldFunds(eq(sourceId), any(Money.class));
        verify(ledgerService, never()).recordTransfer(any(), any(), any(), any(), any(), anyBoolean());
        verify(outboxPublisher).publishEvent(eq("TRANSACTION"), any(), eq("PAYMENT_COMPENSATED"), any());
    }
}
