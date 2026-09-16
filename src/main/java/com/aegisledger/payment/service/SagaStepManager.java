package com.aegisledger.payment.service;

import com.aegisledger.account.service.AccountService;
import com.aegisledger.core.domain.Money;
import com.aegisledger.core.domain.SystemAccounts;
import com.aegisledger.ledger.service.DoubleEntryLedgerService;
import com.aegisledger.outbox.service.OutboxPublisherService;
import com.aegisledger.payment.domain.SagaStep;
import com.aegisledger.payment.domain.Transaction;
import com.aegisledger.payment.domain.TransactionStatus;
import com.aegisledger.payment.dto.TransferRequest;
import com.aegisledger.payment.repository.TransactionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Executes isolated transactional steps for Saga orchestration with REQUIRES_NEW propagation.
 */
@Component
public class SagaStepManager {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final DoubleEntryLedgerService ledgerService;
    private final OutboxPublisherService outboxPublisher;

    public SagaStepManager(
        TransactionRepository transactionRepository,
        AccountService accountService,
        DoubleEntryLedgerService ledgerService,
        OutboxPublisherService outboxPublisher
    ) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.ledgerService = ledgerService;
        this.outboxPublisher = outboxPublisher;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction initiateTransaction(UUID txId, TransferRequest request) {
        Transaction tx = new Transaction(
            txId,
            request.idempotencyKey(),
            request.sourceAccountId(),
            request.destinationAccountId(),
            request.amount(),
            request.currency()
        );
        return transactionRepository.save(tx);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction holdFunds(UUID txId, UUID sourceAccountId, Money amount) {
        accountService.holdFunds(sourceAccountId, amount);
        Transaction tx = transactionRepository.findById(txId).orElseThrow();
        tx.transition(TransactionStatus.EXECUTING, SagaStep.FUNDS_HELD);
        return transactionRepository.save(tx);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction markFraudRejected(UUID txId, UUID sourceAccountId, Money amount, String reason) {
        accountService.releaseHeldFunds(sourceAccountId, amount);
        Transaction tx = transactionRepository.findById(txId).orElseThrow();
        tx.fail(reason);
        Transaction saved = transactionRepository.save(tx);
        outboxPublisher.publishEvent("TRANSACTION", txId.toString(), "PAYMENT_REJECTED", saved);
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFraudPassed(UUID txId) {
        Transaction tx = transactionRepository.findById(txId).orElseThrow();
        tx.setSagaStep(SagaStep.FRAUD_EVALUATED);
        transactionRepository.save(tx);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction compensate(UUID txId, UUID sourceAccountId, Money amount, String failureReason) {
        accountService.releaseHeldFunds(sourceAccountId, amount);
        Transaction tx = transactionRepository.findById(txId).orElseThrow();
        tx.compensate(failureReason);
        Transaction saved = transactionRepository.save(tx);
        outboxPublisher.publishEvent("TRANSACTION", txId.toString(), "PAYMENT_COMPENSATED", saved);
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction commitSuccess(
        UUID txId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        Money amount,
        String description
    ) {
        ledgerService.recordTransfer(
            txId,
            sourceAccountId,
            destinationAccountId,
            amount,
            description,
            true
        );
        Transaction tx = transactionRepository.findById(txId).orElseThrow();
        tx.transition(TransactionStatus.COMPLETED, SagaStep.COMMITTED);
        Transaction saved = transactionRepository.save(tx);
        outboxPublisher.publishEvent("TRANSACTION", txId.toString(), "PAYMENT_COMPLETED", saved);
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction commitSuccessWithFee(
        UUID txId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        Money transferAmount,
        Money feeAmount,
        String description
    ) {
        ledgerService.recordTransfer(
            txId,
            sourceAccountId,
            destinationAccountId,
            transferAmount,
            description,
            true
        );
        if (feeAmount != null && feeAmount.isPositive()) {
            ledgerService.recordTransfer(
                txId,
                sourceAccountId,
                SystemAccounts.FEE_REVENUE_ACCOUNT_ID,
                feeAmount,
                "Transfer fee for tx " + txId,
                true
            );
        }
        Transaction tx = transactionRepository.findById(txId).orElseThrow();
        tx.transition(TransactionStatus.COMPLETED, SagaStep.COMMITTED);
        Transaction saved = transactionRepository.save(tx);
        outboxPublisher.publishEvent("TRANSACTION", txId.toString(), "PAYMENT_COMPLETED", saved);
        return saved;
    }
}