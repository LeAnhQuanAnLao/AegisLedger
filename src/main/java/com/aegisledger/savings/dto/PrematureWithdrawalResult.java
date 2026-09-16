package com.aegisledger.savings.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PrematureWithdrawalResult(
    UUID savingsId,
    BigDecimal principalAmount,
    BigDecimal forfeitedTermInterest,
    BigDecimal actualNonTermInterestPaid,
    BigDecimal totalPayout,
    String message
) {
}
