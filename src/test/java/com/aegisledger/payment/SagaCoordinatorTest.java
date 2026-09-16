package com.aegisledger.payment;

import com.aegisledger.core.domain.Currency;
import com.aegisledger.core.domain.Money;
import com.aegisledger.core.exception.FraudDetectedException;
import com.aegisledger.feelimit.dto.FeeCalculationResult;
import com.aegisledger.feelimit.service.DailyLimitService;
import com.aegisledger.feelimit.service.FeeCalculationService;
import com.aegisledger.fraud.domain.FraudCheckContext;
import com.aegisledger.fraud.domain.FraudCheckResult;
import com.aegisledger.fraud.service.FraudEvaluationService;
import com.aegisledger.payment.domain.SagaStep;
import com.aegisledger.payment.domain.Transaction;
import com.aegisledger.payment.domain.TransactionStatus;
import com.aegisledger.payment.dto.TransferRequest;
import com.aegisledger.payment.dto.TransferResponse;
import com.aegisledger.payment.service.ExternalSwitchService;
import com.aegisledger.payment.service.ExternalSwitchService.SwitchResponse;
import com.aegisledger.payment.service.SagaCoordinator;
import com.aegisledger.payment.service.SagaStepManager;
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
    private SagaStepManager sagaStepManager;
    @Mock
    private FraudEvaluationService fraudEvaluationService;
    @Mock
    private ExternalSwitchService externalSwitchService;
    @Mock
    private FeeCalculationService feeCalculationService;
    @Mock
    private DailyLimitService dailyLimitService;

    @InjectMocks
    private SagaCoordinator sagaCoordinator;

    private UUID sourceId;
    private UUID destId;
    private TransferRequest request;
    private Transaction initialTx;

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
        initialTx = new Transaction(
            UUID.randomUUID(),
            request.idempotencyKey(),
            sourceId,
            destId,
            request.amount(),
            request.currency()
        );
        when(feeCalculationService.calculateFee(any(Money.class))).thenAnswer(inv -> {
            Money amt = inv.getArgument(0);
            return FeeCalculationResult.of(amt, Money.of(0.50, amt.getCurrency()));
        });
        when(sagaStepManager.initiateTransaction(any(), eq(request))).thenReturn(initialTx);
        when(sagaStepManager.holdFunds(any(), eq(sourceId), any(Money.class))).thenReturn(initialTx);
    }

    @Test
    @DisplayName("Should successfully execute all Saga steps to COMPLETED state and record daily limit usage")
    void testHappyPathExecution() {
        Transaction completedTx = new Transaction(
            initialTx.getId(),
            request.idempotencyKey(),
            sourceId,
            destId,
            request.amount(),
            request.currency()
        );
        completedTx.transition(TransactionStatus.COMPLETED, SagaStep.COMMITTED);

        when(fraudEvaluationService.evaluate(any(FraudCheckContext.class))).thenReturn(FraudCheckResult.pass());
        when(externalSwitchService.dispatchTransfer(any(), eq(sourceId), eq(destId), any(Money.class)))
            .thenReturn(new SwitchResponse(true, "REF-123", null));
        when(sagaStepManager.commitSuccessWithFee(any(), eq(sourceId), eq(destId), any(Money.class), any(Money.class), eq("Payment for services")))
            .thenReturn(completedTx);

        TransferResponse response = sagaCoordinator.executeTransfer(request);

        assertNotNull(response);
        assertEquals(TransactionStatus.COMPLETED, response.status());
        assertEquals(SagaStep.COMMITTED, response.currentStep());

        verify(dailyLimitService).validateLimit(eq(sourceId), any(Money.class));
        verify(sagaStepManager).holdFunds(any(), eq(sourceId), any(Money.class));
        verify(fraudEvaluationService).evaluate(any(FraudCheckContext.class));
        verify(sagaStepManager).markFraudPassed(any());
        verify(sagaStepManager).commitSuccessWithFee(any(), eq(sourceId), eq(destId), any(Money.class), any(Money.class), eq("Payment for services"));
        verify(dailyLimitService).recordUsage(eq(sourceId), any(Money.class));
    }

    @Test
    @DisplayName("Should abort and persist FAILED transaction when fraud screening fails")
    void testFraudRejectedThrowsExceptionAndPersistsFailed() {
        when(fraudEvaluationService.evaluate(any(FraudCheckContext.class)))
            .thenReturn(FraudCheckResult.rejected(85, List.of("Velocity limit exceeded")));

        assertThrows(FraudDetectedException.class, () -> sagaCoordinator.executeTransfer(request));

        verify(sagaStepManager).holdFunds(any(), eq(sourceId), any(Money.class));
        verify(sagaStepManager).markFraudRejected(any(), eq(sourceId), any(Money.class), contains("Velocity limit exceeded"));
        verify(externalSwitchService, never()).dispatchTransfer(any(), any(), any(), any());
        verify(sagaStepManager, never()).commitSuccessWithFee(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should trigger Saga Compensation when external switch fails")
    void testExternalSwitchFailureTriggersCompensation() {
        Transaction compensatedTx = new Transaction(
            initialTx.getId(),
            request.idempotencyKey(),
            sourceId,
            destId,
            request.amount(),
            request.currency()
        );
        compensatedTx.compensate("SWITCH_TIMEOUT");

        when(fraudEvaluationService.evaluate(any(FraudCheckContext.class))).thenReturn(FraudCheckResult.pass());
        when(externalSwitchService.dispatchTransfer(any(), eq(sourceId), eq(destId), any(Money.class)))
            .thenReturn(new SwitchResponse(false, null, "SWITCH_TIMEOUT: Partner network unreachable"));
        when(sagaStepManager.compensate(any(), eq(sourceId), any(Money.class), anyString()))
            .thenReturn(compensatedTx);

        TransferResponse response = sagaCoordinator.executeTransfer(request);

        assertNotNull(response);
        assertEquals(TransactionStatus.COMPENSATED, response.status());
        assertEquals(SagaStep.COMPENSATED, response.currentStep());

        verify(sagaStepManager).holdFunds(any(), eq(sourceId), any(Money.class));
        verify(sagaStepManager).compensate(any(), eq(sourceId), any(Money.class), contains("SWITCH_TIMEOUT"));
        verify(sagaStepManager, never()).commitSuccessWithFee(any(), any(), any(), any(), any(), any());
    }
}
