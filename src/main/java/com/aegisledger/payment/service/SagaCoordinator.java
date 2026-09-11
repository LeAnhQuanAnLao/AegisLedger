package com.aegisledger.payment.service;

import com.aegisledger.account.service.AccountService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Saga Coordinator managing distributed transfer workflows and compensation.
 */
@Service
public class SagaCoordinator {

    private static final Logger log = LoggerFactory.getLogger(SagaCoordinator.class);

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final FraudEvaluationService fraudEvaluationService;
    private final ExternalSwitchService externalSwitchService;
    private final DoubleEntryLedgerService ledgerService;
    private final OutboxPublisherService outboxPublisher;

    public SagaCoordinator(
        TransactionRepository transactionRepository,
        AccountService accountService,
        FraudEvaluationService fraudEvaluationService,
        ExternalSwitchService externalSwitchService,
        DoubleEntryLedgerService ledgerService,
        OutboxPublisherService outboxPublisher
    ) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.fraudEvaluationService = fraudEvaluationService;
        this.externalSwitchService = externalSwitchService;
        this.ledgerService = ledgerService;
        this.outboxPublisher = outboxPublisher;
    }

    @Transactional
    public TransferResponse executeTransfer(TransferRequest request) {
        Money transferAmount = Money.of(request.amount(), request.currency());
        UUID txId = UUID.randomUUID();

        // 1. Initialize Transaction record
        Transaction tx = new Transaction(
            txId,
            request.idempotencyKey(),
            request.sourceAccountId(),
            request.destinationAccountId(),
            request.amount(),
            request.currency()
        );
        tx = transactionRepository.save(tx);

        // 2. Step 1: Hold funds on source account
        accountService.holdFunds(request.sourceAccountId(), transferAmount);
        tx.transition(TransactionStatus.EXECUTING, SagaStep.FUNDS_HELD);
        transactionRepository.save(tx);

        // 3. Step 2: Fraud Evaluation
        FraudCheckResult fraudResult = fraudEvaluationService.evaluate(
            FraudCheckContext.of(request.sourceAccountId(), transferAmount)
        );

        if (fraudResult.isRejected()) {
            accountService.releaseHeldFunds(request.sourceAccountId(), transferAmount);
            tx.fail("Fraud screening rejected transaction");
            transactionRepository.save(tx);
            throw new FraudDetectedException(fraudResult.riskScore(), fraudResult.reasons());
        }
        tx.setSagaStep(SagaStep.FRAUD_EVALUATED);
        transactionRepository.save(tx);

        // 4. Step 3: External Switch Dispatch
        var switchResult = externalSwitchService.dispatchTransfer(
            txId, request.sourceAccountId(), request.destinationAccountId(), transferAmount
        );

        if (!switchResult.success()) {
            log.warn("External switch failed for tx {}. Initiating Saga Compensation...", txId);
            accountService.releaseHeldFunds(request.sourceAccountId(), transferAmount);
            tx.compensate(switchResult.errorMessage());
            transactionRepository.save(tx);
            outboxPublisher.publishEvent("TRANSACTION", txId.toString(), "PAYMENT_COMPENSATED", tx);
            return TransferResponse.fromTransaction(tx, "Transfer failed at switch and was compensated: " + switchResult.errorMessage());
        }
        tx.setSagaStep(SagaStep.SWITCH_PROCESSED);
        transactionRepository.save(tx);

        // 5. Step 4: Commit Double-Entry Ledger
        ledgerService.recordTransfer(
            txId,
            request.sourceAccountId(),
            request.destinationAccountId(),
            transferAmount,
            request.description(),
            true
        );

        // 6. Finalize Saga
        tx.transition(TransactionStatus.COMPLETED, SagaStep.COMMITTED);
        Transaction completedTx = transactionRepository.save(tx);
        outboxPublisher.publishEvent("TRANSACTION", txId.toString(), "PAYMENT_COMPLETED", completedTx);

        return TransferResponse.fromTransaction(completedTx, "Payment completed successfully");
    }
}
