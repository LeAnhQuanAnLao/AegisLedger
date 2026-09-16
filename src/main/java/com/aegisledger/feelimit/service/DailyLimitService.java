package com.aegisledger.feelimit.service;

import com.aegisledger.core.domain.Money;
import com.aegisledger.feelimit.domain.DailyLimitConfig;
import com.aegisledger.feelimit.dto.DailyLimitStatusDto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service managing daily spending limit validations and usage tracking.
 */
public interface DailyLimitService {

    /**
     * Validates if the proposed transaction amount exceeds the remaining daily limit for the account.
     *
     * @param accountId Account ID initiating the transfer.
     * @param amount    Transfer amount to be validated.
     */
    void validateLimit(UUID accountId, Money amount);

    /**
     * Records transaction amount against the daily spent tally for today.
     *
     * @param accountId Account ID that executed the transfer.
     * @param amount    Amount spent.
     */
    void recordUsage(UUID accountId, Money amount);

    /**
     * Configures or updates the daily transaction limit for an account.
     */
    DailyLimitConfig configureLimit(UUID accountId, BigDecimal dailyLimit);

    /**
     * Retrieves current daily limit status and remaining balance for an account.
     */
    DailyLimitStatusDto getLimitStatus(UUID accountId);
}
