package com.aegisledger.payment.domain;

import com.aegisledger.core.domain.BaseEntity;
import com.aegisledger.core.domain.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Transaction entity recording saga progression, participant accounts, and status.
 */
@Entity
@Table(name = "transactions")
public class Transaction extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    @Column(name = "destination_account_id", nullable = false)
    private UUID destinationAccountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "saga_step", nullable = false, length = 30)
    private SagaStep sagaStep;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    public Transaction() {
    }

    public Transaction(UUID id, String idempotencyKey, UUID sourceAccountId, UUID destinationAccountId,
                       BigDecimal amount, Currency currency) {
        this.id = Objects.requireNonNull(id, "Transaction ID cannot be null");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
        this.sourceAccountId = Objects.requireNonNull(sourceAccountId, "Source account ID cannot be null");
        this.destinationAccountId = Objects.requireNonNull(destinationAccountId, "Destination account ID cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.currency = Objects.requireNonNull(currency, "Currency cannot be null");
        this.status = TransactionStatus.PENDING;
        this.sagaStep = SagaStep.STARTED;
    }

    public Transaction(UUID id, String idempotencyKey, UUID sourceAccountId, UUID destinationAccountId,
                       BigDecimal amount, Currency currency, TransactionStatus status, SagaStep sagaStep,
                       String failureReason) {
        this.id = Objects.requireNonNull(id, "Transaction ID cannot be null");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
        this.sourceAccountId = Objects.requireNonNull(sourceAccountId, "Source account ID cannot be null");
        this.destinationAccountId = Objects.requireNonNull(destinationAccountId, "Destination account ID cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.currency = Objects.requireNonNull(currency, "Currency cannot be null");
        this.status = status != null ? status : TransactionStatus.PENDING;
        this.sagaStep = sagaStep != null ? sagaStep : SagaStep.STARTED;
        this.failureReason = failureReason;
    }

    public void transition(TransactionStatus newStatus, SagaStep newStep) {
        this.status = newStatus;
        this.sagaStep = newStep;
    }

    public void fail(String reason) {
        this.status = TransactionStatus.FAILED;
        this.failureReason = reason;
    }

    public void compensate(String reason) {
        this.status = TransactionStatus.COMPENSATED;
        this.sagaStep = SagaStep.COMPENSATED;
        this.failureReason = reason;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public UUID getSourceAccountId() { return sourceAccountId; }
    public void setSourceAccountId(UUID sourceAccountId) { this.sourceAccountId = sourceAccountId; }
    public UUID getDestinationAccountId() { return destinationAccountId; }
    public void setDestinationAccountId(UUID destinationAccountId) { this.destinationAccountId = destinationAccountId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public SagaStep getSagaStep() { return sagaStep; }
    public void setSagaStep(SagaStep sagaStep) { this.sagaStep = sagaStep; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
