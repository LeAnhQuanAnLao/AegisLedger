package com.aegisledger.lending.domain;

/**
 * Lifecycle states of a LoanContract.
 */
public enum LoanStatus {
    PENDING_APPROVAL,
    ACTIVE,
    FULLY_PAID,
    DEFAULTED
}
