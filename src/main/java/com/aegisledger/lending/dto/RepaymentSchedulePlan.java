package com.aegisledger.lending.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RepaymentSchedulePlan(
    int installmentNumber,
    LocalDate dueDate,
    BigDecimal principalDue,
    BigDecimal interestDue,
    BigDecimal totalDue,
    BigDecimal remainingPrincipalAfter
) {
}
