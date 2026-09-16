package com.aegisledger.savings.dto;

import com.aegisledger.savings.domain.RolloverOption;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload to open a new online savings account.
 */
public record OpenSavingsRequest(
    @NotNull(message = "Account ID cannot be null")
    UUID accountId,

    @NotNull(message = "Principal amount cannot be null")
    @DecimalMin(value = "1.0", message = "Minimum deposit amount is 1.00")
    BigDecimal principalAmount,

    @Min(value = 0, message = "Term months cannot be negative")
    @Max(value = 12, message = "Term months cannot exceed 12")
    int termMonths,

    @NotNull(message = "Rollover option cannot be null")
    RolloverOption rolloverOption
) {
}
