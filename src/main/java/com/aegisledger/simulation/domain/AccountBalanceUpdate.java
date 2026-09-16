package com.aegisledger.simulation.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * High-performance batch payload for updating account balance state.
 */
public record AccountBalanceUpdate(
    UUID accountId,
    BigDecimal balance,
    BigDecimal lockedBalance,
    BigDecimal availableBalance
) {}
