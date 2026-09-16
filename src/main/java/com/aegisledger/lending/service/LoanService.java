package com.aegisledger.lending.service;

import com.aegisledger.lending.dto.ApplyLoanRequest;
import com.aegisledger.lending.dto.LoanDto;
import com.aegisledger.lending.dto.LoanRepaymentScheduleDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service contract for loan applications, auto-disbursement, and auto-debit collection.
 */
public interface LoanService {

    LoanDto applyAndDisburseLoan(ApplyLoanRequest request);

    LoanDto getLoan(UUID loanId);

    List<LoanDto> getLoansByAccount(UUID accountId);

    List<LoanRepaymentScheduleDto> getRepaymentSchedule(UUID loanId);

    int processAutoDebit(LocalDate targetDate);
}
