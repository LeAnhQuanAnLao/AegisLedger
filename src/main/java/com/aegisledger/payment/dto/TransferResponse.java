package com.aegisledger.payment.dto;

import com.aegisledger.payment.domain.SagaStep;
import com.aegisledger.payment.domain.Transaction;
import com.aegisledger.payment.domain.TransactionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Transfer response payload containing transaction state and saga step.
 */
public record TransferResponse(
    UUID transactionId,
    String idempotencyKey,
    TransactionStatus status,
    SagaStep currentStep,
    String message,
    Instant timestamp
) {
    public static TransferResponse fromTransaction(Transaction tx, String message) {
        return new TransferResponse(
            tx.getId(),
            tx.getIdempotencyKey(),
            tx.getStatus(),
            tx.getSagaStep(),
            message,
            Instant.now()
        );
    }
}
