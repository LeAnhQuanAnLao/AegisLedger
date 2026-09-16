package com.aegisledger.simulation.service;

import com.aegisledger.account.domain.Account;
import com.aegisledger.core.domain.Currency;
import com.aegisledger.core.domain.SystemAccounts;
import com.aegisledger.ledger.domain.EntryType;
import com.aegisledger.ledger.domain.LedgerEntry;
import com.aegisledger.lending.domain.InstallmentStatus;
import com.aegisledger.lending.domain.LoanContract;
import com.aegisledger.lending.domain.LoanRepaymentSchedule;
import com.aegisledger.lending.domain.LoanStatus;
import com.aegisledger.lending.dto.RepaymentSchedulePlan;
import com.aegisledger.lending.service.LoanAmortizationCalculator;
import com.aegisledger.payment.domain.SagaStep;
import com.aegisledger.payment.domain.Transaction;
import com.aegisledger.payment.domain.TransactionStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Sub-simulator for loan applications, disbursements from Treasury, and installment repayment sweeps.
 */
@Component
public class DayLoanSimulator {

    private final LoanAmortizationCalculator calculator;
    private final Random random = new Random();

    public DayLoanSimulator(LoanAmortizationCalculator calculator) {
        this.calculator = calculator;
    }

    public record LoanBatchResult(
        List<LoanContract> newLoans,
        List<LoanRepaymentSchedule> newSchedules,
        List<LoanRepaymentSchedule> updatedSchedules,
        List<Transaction> transactions,
        List<LedgerEntry> ledgerEntries,
        BigDecimal totalDisbursed,
        BigDecimal totalRepaid,
        Set<UUID> modifiedAccountIds
    ) {}

    public LoanBatchResult simulateLoans(
        LocalDate date,
        int count,
        List<Account> accounts,
        Map<UUID, Account> lookup,
        Account treasury,
        Account interestInc,
        List<LoanRepaymentSchedule> pendingSchedules
    ) {
        Instant now = Instant.now();
        List<LoanContract> newLoans = new ArrayList<>(count);
        List<LoanRepaymentSchedule> newSchedules = new ArrayList<>(count * 6);
        List<LoanRepaymentSchedule> updatedSchedules = new ArrayList<>();
        List<Transaction> txs = new ArrayList<>(count * 2);
        List<LedgerEntry> ledgers = new ArrayList<>(count * 5);
        Set<UUID> modified = new HashSet<>();
        BigDecimal totalDisbursed = BigDecimal.ZERO;

        int totalUsers = accounts.size();
        for (int i = 0; i < count; i++) {
            Account user = accounts.get(random.nextInt(totalUsers));
            BigDecimal loanAmt = BigDecimal.valueOf(500 + random.nextInt(1500)).setScale(4, RoundingMode.HALF_EVEN);
            if (treasury.getAvailableBalance().compareTo(loanAmt) < 0) break;

            int termMonths = random.nextBoolean() ? 3 : 6;
            BigDecimal rate = new BigDecimal("0.1000");
            LoanContract loan = disburseLoan(user, treasury, loanAmt, rate, termMonths, date, now, txs, ledgers);
            newLoans.add(loan);
            modified.add(user.getId());
            modified.add(treasury.getId());
            totalDisbursed = totalDisbursed.add(loanAmt);

            List<LoanRepaymentSchedule> schedules = createSchedules(loan, date, now);
            newSchedules.addAll(schedules);
            pendingSchedules.addAll(schedules);
        }

        BigDecimal totalRepaid = sweepRepayments(date, now, pendingSchedules, lookup, treasury, interestInc,
            txs, ledgers, updatedSchedules, modified);

        return new LoanBatchResult(newLoans, newSchedules, updatedSchedules, txs, ledgers,
            totalDisbursed, totalRepaid, modified);
    }

    private LoanContract disburseLoan(Account user, Account treasury, BigDecimal amt, BigDecimal rate,
                                      int term, LocalDate date, Instant now,
                                      List<Transaction> txs, List<LedgerEntry> ledgers) {
        treasury.setBalance(treasury.getBalance().subtract(amt));
        treasury.setAvailableBalance(treasury.getAvailableBalance().subtract(amt));
        user.setBalance(user.getBalance().add(amt));
        user.setAvailableBalance(user.getAvailableBalance().add(amt));

        UUID txId = UUID.randomUUID();
        Transaction tx = new Transaction(txId, "LOAN-DISB-" + UUID.randomUUID(), treasury.getId(), user.getId(),
            amt, Currency.USD, TransactionStatus.COMPLETED, SagaStep.COMMITTED, null);
        tx.setCreatedAt(now);
        tx.setUpdatedAt(now);
        tx.setVersion(0L);
        txs.add(tx);

        ledgers.add(new LedgerEntry(UUID.randomUUID(), txId, treasury.getId(), EntryType.DEBIT,
            amt, treasury.getBalance(), "Loan Disbursement Debit"));
        ledgers.add(new LedgerEntry(UUID.randomUUID(), txId, user.getId(), EntryType.CREDIT,
            amt, user.getBalance(), "Loan Disbursement Credit"));

        LoanContract loan = new LoanContract(UUID.randomUUID(), user.getId(),
            "LOAN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(), amt, rate, term);
        loan.setCreatedAt(now);
        loan.setUpdatedAt(now);
        loan.setVersion(0L);
        return loan;
    }

    private List<LoanRepaymentSchedule> createSchedules(LoanContract loan, LocalDate date, Instant now) {
        List<RepaymentSchedulePlan> plans = calculator.generateSchedule(loan.getPrincipalAmount(),
            loan.getInterestRate(), loan.getTermMonths(), date);

        List<LoanRepaymentSchedule> schedules = new ArrayList<>(plans.size());
        for (RepaymentSchedulePlan p : plans) {
            LoanRepaymentSchedule s = new LoanRepaymentSchedule(UUID.randomUUID(), loan.getId(), p.installmentNumber(),
                p.dueDate(), p.principalDue(), p.interestDue());
            s.setCreatedAt(now);
            s.setUpdatedAt(now);
            s.setVersion(0L);
            schedules.add(s);
        }
        return schedules;
    }

    private BigDecimal sweepRepayments(LocalDate date, Instant now, List<LoanRepaymentSchedule> pending,
                                       Map<UUID, Account> lookup, Account treasury, Account interestInc,
                                       List<Transaction> txs, List<LedgerEntry> ledgers,
                                       List<LoanRepaymentSchedule> updated, Set<UUID> modified) {
        BigDecimal total = BigDecimal.ZERO;
        Iterator<LoanRepaymentSchedule> it = pending.iterator();
        while (it.hasNext()) {
            LoanRepaymentSchedule s = it.next();
            if (s.getStatus() == InstallmentStatus.PENDING && !s.getDueDate().isAfter(date)) {
                s.markPaid();
                updated.add(s);
                it.remove();
                total = total.add(s.getTotalDue());
            }
        }
        return total;
    }
}
