package com.aegisledger.eod.domain;

/**
 * Status of the End-Of-Day reconciliation process.
 */
public enum ReconciliationStatus {
    BALANCED,
    DISCREPANCY_DETECTED,
    FAILED
}
