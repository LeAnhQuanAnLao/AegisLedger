package com.aegisledger.feelimit.service;

import com.aegisledger.core.domain.Money;
import com.aegisledger.feelimit.dto.FeeCalculationResult;

/**
 * Service contract for calculating transaction and service fees.
 */
public interface FeeCalculationService {

    /**
     * Calculates the transfer fee for a given transfer amount.
     *
     * @param amount The principal transfer amount.
     * @return FeeCalculationResult containing transfer amount, fee amount, and total debit.
     */
    FeeCalculationResult calculateFee(Money amount);
}
