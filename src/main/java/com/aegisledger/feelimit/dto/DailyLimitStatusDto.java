package com.aegisledger.feelimit.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Status DTO showing daily transaction limit details for an account.
 */
public record DailyLimitStatusDto(
    UUID accountId,
    LocalDate date,
    BigDecimal configuredLimit,
    BigDecimal totalSpentToday,
    BigDecimal remainingLimit
) {
}
