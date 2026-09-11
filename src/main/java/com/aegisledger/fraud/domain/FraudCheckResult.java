package com.aegisledger.fraud.domain;

import java.util.List;

/**
 * Outcome payload of a fraud screening evaluation.
 */
public record FraudCheckResult(
    FraudStatus status,
    int riskScore,
    List<String> reasons
) {
    public static FraudCheckResult pass() {
        return new FraudCheckResult(FraudStatus.PASSED, 0, List.of());
    }

    public static FraudCheckResult suspicious(int score, List<String> reasons) {
        return new FraudCheckResult(FraudStatus.SUSPICIOUS, score, reasons);
    }

    public static FraudCheckResult rejected(int score, List<String> reasons) {
        return new FraudCheckResult(FraudStatus.REJECTED, score, reasons);
    }

    public boolean isRejected() {
        return this.status == FraudStatus.REJECTED;
    }
}
