package com.aegisledger.payment.service;

import com.aegisledger.core.domain.Money;
import com.aegisledger.core.exception.FraudDetectedException;
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
 * Saga Coordinator managing distributed transfer workflows and compensation.
 * Transaction boundaries are decoupled via SagaStepManager to prevent DB connection pool starvation.
 */
@Service
public class SagaCoordinator {

    private static final Logger log = LoggerFactory.getLogger(SagaCoordinator.class);

    private final SagaStepManager sagaStepManager;
    private final FraudEvaluationService fraudEvaluationService;
    private final ExternalSwitchService externalSwitchService;

    public SagaCoordinator(
        SagaStepManager sagaStepManager,
        FraudEvaluationService fraudEvaluationService,
        ExternalSwitchService externalSwitchService
    ) {
        this.sagaStepManager = sagaStepManager;
        this.fraudEvaluationService = fraudEvaluationService;
        this.externalSwitchService = externalSwitchService;
    }

    public TransferResponse executeTransfer(TransferRequest request) {
        Money transferAmount = Money.of(request.amount(), request.currency());
        UUID txId = UUID.randomUUID();

        // 1. Initialize Transaction record (Committed in isolated Tx)
        Transaction tx = sagaStepManager.initiateTransaction(txId, request);

        // 2. Step 1: Hold funds on source account (Commits and releases DB lock immediately!)
        tx = sagaStepManager.holdFunds(txId, request.sourceAccountId(), transferAmount);

        // 3. Step 2: Fraud Evaluation
        FraudCheckResult fraudResult = fraudEvaluationService.evaluate(
            FraudCheckContext.of(request.sourceAccountId(), transferAmount)
        );

        if (fraudResult.isRejected()) {
            String reason = "Fraud screening rejected transaction: " + String.join("; ", fraudResult.reasons());
            sagaStepManager.markFraudRejected(txId, request.sourceAccountId(), transferAmount, reason);
            throw new FraudDetectedException(fraudResult.riskScore(), fraudResult.reasons());
        }
        sagaStepManager.markFraudPassed(txId);

        // 4. Step 3: External Switch Dispatch (Non-blocking I/O, ZERO DB connection held!)
        var switchResult = externalSwitchService.dispatchTransfer(
            txId, request.sourceAccountId(), request.destinationAccountId(), transferAmount
        );

        if (!switchResult.success()) {
            log.warn("External switch failed for tx {}. Initiating Saga Compensation...", txId);
            Transaction compensatedTx = sagaStepManager.compensate(
                txId, request.sourceAccountId(), transferAmount, switchResult.errorMessage()
            );
            return TransferResponse.fromTransaction(
                compensatedTx,
                "Transfer failed at switch and was compensated: " + switchResult.errorMessage()
            );
        }

        // 5. Step 4: Commit Double-Entry Ledger and Finalize Saga
        Transaction completedTx = sagaStepManager.commitSuccess(
            txId,
            request.sourceAccountId(),
            request.destinationAccountId(),
            transferAmount,
            request.description()
        );

        return TransferResponse.fromTransaction(completedTx, "Payment completed successfully");
    }
}
