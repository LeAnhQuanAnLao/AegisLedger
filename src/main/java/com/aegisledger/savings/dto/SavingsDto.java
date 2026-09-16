package com.aegisledger.savings.dto;

import com.aegisledger.savings.domain.RolloverOption;
import com.aegisledger.savings.domain.SavingsAccount;
import com.aegisledger.savings.domain.SavingsStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SavingsDto(
    UUID id,
    UUID accountId,
    String savingsNumber,
    BigDecimal principalAmount,
    BigDecimal interestRate,
    int termMonths,
    RolloverOption rolloverOption,
    BigDecimal accruedInterest,
    LocalDate startDate,
    LocalDate maturityDate,
    LocalDate lastAccrualDate,
    SavingsStatus status
) {
    public static SavingsDto fromEntity(SavingsAccount entity) {
        return new SavingsDto(
            entity.getId(),
            entity.getAccountId(),
            entity.getSavingsNumber(),
            entity.getPrincipalAmount(),
            entity.getInterestRate(),
            entity.getTermMonths(),
            entity.getRolloverOption(),
            entity.getAccruedInterest(),
            entity.getStartDate(),
            entity.getMaturityDate(),
            entity.getLastAccrualDate(),
            entity.getStatus()
        );
    }
}
