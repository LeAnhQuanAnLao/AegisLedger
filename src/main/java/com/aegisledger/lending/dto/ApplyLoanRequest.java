package com.aegisledger.lending.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request payload to apply and automatically disburse a micro-loan.
 */
public record ApplyLoanRequest(
    @NotNull(message = "Account ID cannot be null")
    UUID accountId,

    @NotNull(message = "Requested amount cannot be null")
    @DecimalMin(value = "10.0", message = "Minimum loan amount is 10.00")
    BigDecimal requestedAmount,

    @Min(value = 1, message = "Minimum term is 1 month")
    @Max(value = 24, message = "Maximum term is 24 months")
    int termMonths
) {
}
