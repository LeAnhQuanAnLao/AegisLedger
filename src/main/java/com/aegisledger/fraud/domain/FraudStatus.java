package com.aegisledger.fraud.domain;

/**
 * Risk classification result from the Fraud Engine.
 */
public enum FraudStatus {
    PASSED,
    SUSPICIOUS,
    REJECTED
}
