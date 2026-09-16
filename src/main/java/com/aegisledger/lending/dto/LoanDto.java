package com.aegisledger.lending.dto;

import com.aegisledger.lending.domain.LoanContract;
import com.aegisledger.lending.domain.LoanStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LoanDto(
    UUID id,
    UUID accountId,
    String loanNumber,
    BigDecimal principalAmount,
    BigDecimal interestRate,
    int termMonths,
    BigDecimal remainingPrincipal,
    LoanStatus status,
    Instant disbursedAt
) {
    public static LoanDto fromEntity(LoanContract entity) {
        return new LoanDto(
            entity.getId(),
            entity.getAccountId(),
            entity.getLoanNumber(),
            entity.getPrincipalAmount(),
            entity.getInterestRate(),
            entity.getTermMonths(),
            entity.getRemainingPrincipal(),
            entity.getStatus(),
            entity.getDisbursedAt()
        );
    }
}
