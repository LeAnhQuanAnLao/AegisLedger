package com.aegisledger.savings.service;

import com.aegisledger.savings.dto.OpenSavingsRequest;
import com.aegisledger.savings.dto.PrematureWithdrawalResult;
import com.aegisledger.savings.dto.SavingsDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for online savings account lifecycle and interest operations.
 */
public interface SavingsService {

    SavingsDto openSavings(OpenSavingsRequest request);

    SavingsDto getSavings(UUID savingsId);

    List<SavingsDto> getSavingsByAccount(UUID accountId);

    int accrueDailyInterest(LocalDate accrualDate);

    int processMaturities(LocalDate date);

    PrematureWithdrawalResult withdrawPrematurely(UUID savingsId);
}
