package com.aegisledger.account.dto;

import com.aegisledger.core.domain.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Request payload for creating a new banking account.
 */
public record CreateAccountRequest(
    @NotBlank(message = "Account number is required")
    String accountNumber,

    @NotBlank(message = "Holder name is required")
    String holderName,

    @NotNull(message = "Currency is required")
    Currency currency,

    @NotNull(message = "Initial deposit amount is required")
    @PositiveOrZero(message = "Initial deposit must be non-negative")
    BigDecimal initialDeposit
) {
}
