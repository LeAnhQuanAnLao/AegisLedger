package com.aegisledger.payment.dto;

import com.aegisledger.core.domain.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Transfer request payload for executing a multi-step payment saga.
 */
public record TransferRequest(
    @NotNull(message = "Source account ID is required")
    UUID sourceAccountId,

    @NotNull(message = "Destination account ID is required")
    UUID destinationAccountId,

    @NotNull(message = "Transfer amount is required")
    @Positive(message = "Transfer amount must be strictly positive")
    BigDecimal amount,

    @NotNull(message = "Currency is required")
    Currency currency,

    @NotBlank(message = "Idempotency key is required")
    String idempotencyKey,

    String description
) {
}
