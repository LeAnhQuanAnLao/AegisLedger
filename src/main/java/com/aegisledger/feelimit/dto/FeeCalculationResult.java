package com.aegisledger.feelimit.dto;

import com.aegisledger.core.domain.Money;

import java.math.BigDecimal;

/**
 * Result DTO containing calculated transfer fee and net total.
 */
public record FeeCalculationResult(
    Money transferAmount,
    Money feeAmount,
    Money totalDebitAmount
) {
    public static FeeCalculationResult of(Money transferAmount, Money feeAmount) {
        Money total = transferAmount.plus(feeAmount);
        return new FeeCalculationResult(transferAmount, feeAmount, total);
    }
}
