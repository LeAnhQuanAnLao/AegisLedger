package com.aegisledger.payment.domain;

/**
 * Checkpoint steps tracked during Saga execution.
 */
public enum SagaStep {
    STARTED,
    FUNDS_HELD,
    FRAUD_EVALUATED,
    SWITCH_PROCESSED,
    COMMITTED,
    COMPENSATED
}
