package com.aegisledger.lending.dto;

import java.math.BigDecimal;

/**
 * Result DTO of an automated credit scoring evaluation.
 */
public record CreditScoreResult(
    int score,
    BigDecimal maxAllowedLoan,
    BigDecimal interestRate,
    boolean approved,
    String reason
) {
}
