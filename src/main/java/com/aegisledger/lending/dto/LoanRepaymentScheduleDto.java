package com.aegisledger.lending.dto;

import com.aegisledger.lending.domain.InstallmentStatus;
import com.aegisledger.lending.domain.LoanRepaymentSchedule;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LoanRepaymentScheduleDto(
    UUID id,
    UUID loanId,
    int installmentNumber,
    LocalDate dueDate,
    BigDecimal principalDue,
    BigDecimal interestDue,
    BigDecimal totalDue,
    BigDecimal principalPaid,
    BigDecimal interestPaid,
    InstallmentStatus status,
    Instant paidAt
) {
    public static LoanRepaymentScheduleDto fromEntity(LoanRepaymentSchedule entity) {
        return new LoanRepaymentScheduleDto(
            entity.getId(),
            entity.getLoanId(),
            entity.getInstallmentNumber(),
            entity.getDueDate(),
            entity.getPrincipalDue(),
            entity.getInterestDue(),
            entity.getTotalDue(),
            entity.getPrincipalPaid(),
            entity.getInterestPaid(),
            entity.getStatus(),
            entity.getPaidAt()
        );
    }
}
