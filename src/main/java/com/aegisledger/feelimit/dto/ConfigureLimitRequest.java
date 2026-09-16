package com.aegisledger.feelimit.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request payload to configure or update the daily transaction limit for an account.
 */
public record ConfigureLimitRequest(
    @NotNull(message = "Account ID cannot be null")
    UUID accountId,

    @NotNull(message = "Daily limit cannot be null")
    @DecimalMin(value = "0.0", inclusive = true, message = "Daily limit must be non-negative")
    BigDecimal dailyLimit
) {
}
