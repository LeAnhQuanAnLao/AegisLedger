package com.aegisledger.payment.service;

import com.aegisledger.core.domain.Money;
import com.aegisledger.core.exception.FraudDetectedException;
import com.aegisledger.feelimit.dto.FeeCalculationResult;
import com.aegisledger.feelimit.service.DailyLimitService;
import com.aegisledger.feelimit.service.FeeCalculationService;
import com.aegisledger.fraud.domain.FraudCheckContext;
import com.aegisledger.fraud.domain.FraudCheckResult;
import com.aegisledger.fraud.service.FraudEvaluationService;
import com.aegisledger.payment.domain.Transaction;
import com.aegisledger.payment.dto.TransferRequest;
import com.aegisledger.payment.dto.TransferResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Saga Coordinator managing distributed transfer workflows, daily limits, fees, and compensation.
 */
@Service
public class SagaCoordinator {

    private static final Logger log = LoggerFactory.getLogger(SagaCoordinator.class);

    private final SagaStepManager sagaStepManager;
    private final FraudEvaluationService fraudEvaluationService;
    private final ExternalSwitchService externalSwitchService;
    private final FeeCalculationService feeCalculationService;
    private final DailyLimitService dailyLimitService;

    public SagaCoordinator(
        SagaStepManager sagaStepManager,
        FraudEvaluationService fraudEvaluationService,
        ExternalSwitchService externalSwitchService,
        FeeCalculationService feeCalculationService,
        DailyLimitService dailyLimitService
    ) {
        this.sagaStepManager = sagaStepManager;
        this.fraudEvaluationService = fraudEvaluationService;
        this.externalSwitchService = externalSwitchService;
        this.feeCalculationService = feeCalculationService;
        this.dailyLimitService = dailyLimitService;
    }

    public TransferResponse executeTransfer(TransferRequest request) {
        Money transferAmount = Money.of(request.amount(), request.currency());
        UUID txId = UUID.randomUUID();

        // 1. Pre-validation: Enforce Daily Spending Limit
        dailyLimitService.validateLimit(request.sourceAccountId(), transferAmount);

        // 2. Calculate Transfer Fee
        FeeCalculationResult feeResult = feeCalculationService.calculateFee(transferAmount);
        Money feeAmount = feeResult.feeAmount();
        Money totalDebitAmount = feeResult.totalDebitAmount();

        // 3. Initialize Transaction record (Committed in isolated Tx)
        Transaction tx = sagaStepManager.initiateTransaction(txId, request);

        // 4. Step 1: Hold funds on source account (covering principal + fee)
        tx = sagaStepManager.holdFunds(txId, request.sourceAccountId(), totalDebitAmount);

        // 5. Step 2: Fraud Evaluation
        FraudCheckResult fraudResult = fraudEvaluationService.evaluate(
            FraudCheckContext.of(request.sourceAccountId(), transferAmount)
        );

        if (fraudResult.isRejected()) {
            String reason = "Fraud screening rejected transaction: " + String.join("; ", fraudResult.reasons());
            sagaStepManager.markFraudRejected(txId, request.sourceAccountId(), totalDebitAmount, reason);
            throw new FraudDetectedException(fraudResult.riskScore(), fraudResult.reasons());
        }
        sagaStepManager.markFraudPassed(txId);

        // 6. Step 3: External Switch Dispatch (Non-blocking I/O, ZERO DB connection held!)
        var switchResult = externalSwitchService.dispatchTransfer(
            txId, request.sourceAccountId(), request.destinationAccountId(), transferAmount
        );

        if (!switchResult.success()) {
            log.warn("External switch failed for tx {}. Initiating Saga Compensation...", txId);
            Transaction compensatedTx = sagaStepManager.compensate(
                txId, request.sourceAccountId(), totalDebitAmount, switchResult.errorMessage()
            );
            return TransferResponse.fromTransaction(
                compensatedTx,
                "Transfer failed at switch and was compensated: " + switchResult.errorMessage()
            );
        }

        // 7. Step 4: Commit Double-Entry Ledger (Transfer + Fee) and Record Daily Limit Usage
        Transaction completedTx = sagaStepManager.commitSuccessWithFee(
            txId,
            request.sourceAccountId(),
            request.destinationAccountId(),
            transferAmount,
            feeAmount,
            request.description()
        );

        dailyLimitService.recordUsage(request.sourceAccountId(), transferAmount);

        return TransferResponse.fromTransaction(completedTx, "Payment completed successfully");
    }
}
