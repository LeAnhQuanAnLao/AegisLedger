package com.aegisledger.lending.service;

import com.aegisledger.account.domain.Account;
import com.aegisledger.account.repository.AccountRepository;
import com.aegisledger.core.domain.Money;
import com.aegisledger.core.domain.SystemAccounts;
import com.aegisledger.core.exception.AccountNotFoundException;
import com.aegisledger.ledger.service.DoubleEntryLedgerService;
import com.aegisledger.lending.domain.InstallmentStatus;
import com.aegisledger.lending.domain.LoanContract;
import com.aegisledger.lending.domain.LoanRepaymentSchedule;
import com.aegisledger.lending.dto.ApplyLoanRequest;
import com.aegisledger.lending.dto.CreditScoreResult;
import com.aegisledger.lending.dto.LoanDto;
import com.aegisledger.lending.dto.LoanRepaymentScheduleDto;
import com.aegisledger.lending.dto.RepaymentSchedulePlan;
import com.aegisledger.lending.exception.LoanNotFoundException;
import com.aegisledger.lending.exception.LoanRejectedException;
import com.aegisledger.lending.repository.LoanContractRepository;
import com.aegisledger.lending.repository.LoanRepaymentScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class LoanServiceImpl implements LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanServiceImpl.class);

    private final LoanContractRepository loanRepository;
    private final LoanRepaymentScheduleRepository scheduleRepository;
    private final AccountRepository accountRepository;
    private final DoubleEntryLedgerService ledgerService;
    private final CreditScoringService creditScoringService;
    private final LoanAmortizationCalculator amortizationCalculator;

    public LoanServiceImpl(
        LoanContractRepository loanRepository,
        LoanRepaymentScheduleRepository scheduleRepository,
        AccountRepository accountRepository,
        DoubleEntryLedgerService ledgerService,
        CreditScoringService creditScoringService,
        LoanAmortizationCalculator amortizationCalculator
    ) {
        this.loanRepository = loanRepository;
        this.scheduleRepository = scheduleRepository;
        this.accountRepository = accountRepository;
        this.ledgerService = ledgerService;
        this.creditScoringService = creditScoringService;
        this.amortizationCalculator = amortizationCalculator;
    }

    @Override
    @Transactional
    public LoanDto applyAndDisburseLoan(ApplyLoanRequest request) {
        Objects.requireNonNull(request, "Request cannot be null");
        Account customer = accountRepository.findById(request.accountId())
            .orElseThrow(() -> new AccountNotFoundException(request.accountId()));

        CreditScoreResult scoreResult = creditScoringService.evaluate(
            request.accountId(), request.requestedAmount(), request.termMonths()
        );
        if (!scoreResult.approved()) {
            throw new LoanRejectedException(scoreResult.score(), scoreResult.reason());
        }

        String loanNumber = "LOAN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Money disburseAmount = Money.of(request.requestedAmount(), customer.getCurrency());

        // 1. Auto-Disburse from Bank Treasury to customer account via Double-Entry Ledger
        ledgerService.recordTransfer(
            UUID.randomUUID(), SystemAccounts.TREASURY_ACCOUNT_ID, customer.getId(),
            disburseAmount, "Disburse loan " + loanNumber, false
        );

        // 2. Persist Loan Contract
        LoanContract loan = new LoanContract(
            UUID.randomUUID(), customer.getId(), loanNumber, disburseAmount.getAmount(),
            scoreResult.interestRate(), request.termMonths()
        );
        LoanContract savedLoan = loanRepository.save(loan);

        // 3. Generate & Persist Amortization Schedule
        List<RepaymentSchedulePlan> plans = amortizationCalculator.generateSchedule(
            savedLoan.getPrincipalAmount(), savedLoan.getInterestRate(), savedLoan.getTermMonths(), LocalDate.now()
        );
        for (RepaymentSchedulePlan p : plans) {
            LoanRepaymentSchedule schedule = new LoanRepaymentSchedule(
                UUID.randomUUID(), savedLoan.getId(), p.installmentNumber(),
                p.dueDate(), p.principalDue(), p.interestDue()
            );
            scheduleRepository.save(schedule);
        }

        log.info("Disbursed loan {} for customer {}: amount={}, rate={}",
            loanNumber, customer.getId(), disburseAmount, scoreResult.interestRate());
        return LoanDto.fromEntity(savedLoan);
    }

    @Override
    @Transactional(readOnly = true)
    public LoanDto getLoan(UUID loanId) {
        return loanRepository.findById(loanId)
            .map(LoanDto::fromEntity)
            .orElseThrow(() -> new LoanNotFoundException(loanId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanDto> getLoansByAccount(UUID accountId) {
        return loanRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
            .map(LoanDto::fromEntity)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanRepaymentScheduleDto> getRepaymentSchedule(UUID loanId) {
        return scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId).stream()
            .map(LoanRepaymentScheduleDto::fromEntity)
            .toList();
    }

    @Override
    @Transactional
    public int processAutoDebit(LocalDate targetDate) {
        List<LoanRepaymentSchedule> dueInstallments = scheduleRepository
            .findByStatusInAndDueDateLessThanEqualOrderByDueDateAsc(
                Set.of(InstallmentStatus.PENDING, InstallmentStatus.OVERDUE), targetDate
            );

        int processed = 0;
        for (LoanRepaymentSchedule schedule : dueInstallments) {
            LoanContract loan = loanRepository.findById(schedule.getLoanId()).orElse(null);
            if (loan == null) continue;

            Account customer = accountRepository.findById(loan.getAccountId()).orElse(null);
            if (customer == null) continue;

            Money totalDue = Money.of(schedule.getTotalDue(), customer.getCurrency());
            if (customer.getAvailableBalanceMoney().isLessThan(totalDue)) {
                schedule.markOverdue();
                scheduleRepository.save(schedule);
                log.warn("Auto-debit failed for installment {} of loan {}: insufficient balance",
                    schedule.getInstallmentNumber(), loan.getLoanNumber());
                continue;
            }

            // Transfer Principal portion to Treasury
            Money principal = Money.of(schedule.getPrincipalDue(), customer.getCurrency());
            ledgerService.recordTransfer(
                UUID.randomUUID(), customer.getId(), SystemAccounts.TREASURY_ACCOUNT_ID,
                principal, "Auto-debit principal " + loan.getLoanNumber() + " #" + schedule.getInstallmentNumber(), false
            );

            // Transfer Interest portion to Interest Income
            Money interest = Money.of(schedule.getInterestDue(), customer.getCurrency());
            ledgerService.recordTransfer(
                UUID.randomUUID(), customer.getId(), SystemAccounts.INTEREST_INCOME_ACCOUNT_ID,
                interest, "Auto-debit interest " + loan.getLoanNumber() + " #" + schedule.getInstallmentNumber(), false
            );

            schedule.markPaid();
            loan.reducePrincipal(schedule.getPrincipalDue());
            scheduleRepository.save(schedule);
            loanRepository.save(loan);
            processed++;
        }
        return processed;
    }
}
