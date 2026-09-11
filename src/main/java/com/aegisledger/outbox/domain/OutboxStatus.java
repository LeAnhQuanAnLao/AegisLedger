package com.aegisledger.outbox.domain;

/**
 * Delivery status for outbox event records.
 */
public enum OutboxStatus {
    PENDING,
    PROCESSED,
    FAILED
}
