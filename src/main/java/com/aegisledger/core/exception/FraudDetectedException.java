package com.aegisledger.core.exception;

import org.springframework.http.HttpStatus;
import java.util.List;

/**
 * Thrown when a transaction is flagged and rejected by the real-time fraud detection engine.
 */
public class FraudDetectedException extends BusinessException {

    private final int riskScore;
    private final List<String> reasons;

    public FraudDetectedException(int riskScore, List<String> reasons) {
        super("FRAUD_DETECTED", "Transaction rejected due to fraud risk: " + String.join(", ", reasons), HttpStatus.FORBIDDEN);
        this.riskScore = riskScore;
        this.reasons = reasons != null ? List.copyOf(reasons) : List.of();
    }

    public int getRiskScore() {
        return riskScore;
    }

    public List<String> getReasons() {
        return reasons;
    }
}
