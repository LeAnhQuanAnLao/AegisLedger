package com.aegisledger.payment.domain;

/**
 * Lifecycle states of a financial transaction in the Saga workflow.
 */
public enum TransactionStatus {
    PENDING,
    EXECUTING,
    COMPLETED,
    COMPENSATED,
    FAILED
}
