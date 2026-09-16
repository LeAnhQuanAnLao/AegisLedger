package com.aegisledger.savings.service;

import com.aegisledger.account.domain.Account;
import com.aegisledger.account.repository.AccountRepository;
import com.aegisledger.core.domain.Money;
import com.aegisledger.core.domain.SystemAccounts;
import com.aegisledger.core.exception.AccountNotFoundException;
import com.aegisledger.ledger.service.DoubleEntryLedgerService;
import com.aegisledger.savings.domain.RolloverOption;
import com.aegisledger.savings.domain.SavingsAccount;
import com.aegisledger.savings.domain.SavingsStatus;
import com.aegisledger.savings.domain.SavingsTerm;
import com.aegisledger.savings.dto.OpenSavingsRequest;
import com.aegisledger.savings.dto.PrematureWithdrawalResult;
import com.aegisledger.savings.dto.SavingsDto;
import com.aegisledger.savings.exception.SavingsAlreadySettledException;
import com.aegisledger.savings.exception.SavingsNotFoundException;
import com.aegisledger.savings.repository.SavingsAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class SavingsServiceImpl implements SavingsService {

    private static final Logger log = LoggerFactory.getLogger(SavingsServiceImpl.class);

    private final SavingsAccountRepository savingsRepository;
    private final AccountRepository accountRepository;
    private final DoubleEntryLedgerService ledgerService;
    private final SavingsInterestCalculator calculator;

    public SavingsServiceImpl(
        SavingsAccountRepository savingsRepository,
        AccountRepository accountRepository,
        DoubleEntryLedgerService ledgerService,
        SavingsInterestCalculator calculator
    ) {
        this.savingsRepository = savingsRepository;
        this.accountRepository = accountRepository;
        this.ledgerService = ledgerService;
        this.calculator = calculator;
    }

    @Override
    @Transactional
    public SavingsDto openSavings(OpenSavingsRequest request) {
        Objects.requireNonNull(request, "Request cannot be null");
        Account source = accountRepository.findById(request.accountId())
            .orElseThrow(() -> new AccountNotFoundException(request.accountId()));

        Money depositMoney = Money.of(request.principalAmount(), source.getCurrency());
        BigDecimal rate = SavingsTerm.resolveAnnualRate(request.termMonths());
        String savingsNumber = "SAV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        ledgerService.recordTransfer(
            UUID.randomUUID(),
            source.getId(),
            SystemAccounts.SAVINGS_VAULT_ACCOUNT_ID,
            depositMoney,
            "Open savings deposit " + savingsNumber,
            false
        );

        SavingsAccount savings = new SavingsAccount(
            UUID.randomUUID(),
            source.getId(),
            savingsNumber,
            depositMoney.getAmount(),
            rate,
            request.termMonths(),
            request.rolloverOption(),
            LocalDate.now()
        );

        SavingsAccount saved = savingsRepository.save(savings);
        log.info("Opened savings account: number={}, principal={}, rate={}", savingsNumber, depositMoney, rate);
        return SavingsDto.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SavingsDto getSavings(UUID savingsId) {
        return savingsRepository.findById(savingsId)
            .map(SavingsDto::fromEntity)
            .orElseThrow(() -> new SavingsNotFoundException(savingsId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavingsDto> getSavingsByAccount(UUID accountId) {
        return savingsRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
            .map(SavingsDto::fromEntity)
            .toList();
    }

    @Override
    @Transactional
    public int accrueDailyInterest(LocalDate accrualDate) {
        List<SavingsAccount> activeSavings = savingsRepository.findByStatus(SavingsStatus.ACTIVE);
        int count = 0;
        for (SavingsAccount s : activeSavings) {
            if (s.getLastAccrualDate() == null || s.getLastAccrualDate().isBefore(accrualDate)) {
                BigDecimal daily = calculator.calculateDailyInterest(s.getPrincipalAmount(), s.getInterestRate());
                s.addAccruedInterest(daily, accrualDate);
                savingsRepository.save(s);
                count++;
            }
        }
        log.info("Accrued daily interest for {} savings accounts on {}", count, accrualDate);
        return count;
    }

    @Override
    @Transactional
    public int processMaturities(LocalDate date) {
        List<SavingsAccount> matured = savingsRepository.findByStatusAndMaturityDateLessThanEqual(SavingsStatus.ACTIVE, date);
        for (SavingsAccount s : matured) {
            Account customerAcc = accountRepository.findById(s.getAccountId())
                .orElseThrow(() -> new AccountNotFoundException(s.getAccountId()));

            Money principal = Money.of(s.getPrincipalAmount(), customerAcc.getCurrency());
            Money interest = Money.of(s.getAccruedInterest(), customerAcc.getCurrency());

            if (s.getRolloverOption() == RolloverOption.AUTO_SETTLE) {
                settleToCustomer(s, customerAcc, principal, interest);
            } else if (s.getRolloverOption() == RolloverOption.ROLLOVER_PRINCIPAL_AND_INTEREST) {
                rolloverAll(s, customerAcc, interest);
            } else {
                rolloverPrincipalOnly(s, customerAcc, interest);
            }
        }
        return matured.size();
    }

    @Override
    @Transactional
    public PrematureWithdrawalResult withdrawPrematurely(UUID savingsId) {
        SavingsAccount s = savingsRepository.findById(savingsId)
            .orElseThrow(() -> new SavingsNotFoundException(savingsId));

        if (s.getStatus() != SavingsStatus.ACTIVE) {
            throw new SavingsAlreadySettledException(savingsId, s.getStatus().name());
        }

        Account customerAcc = accountRepository.findById(s.getAccountId())
            .orElseThrow(() -> new AccountNotFoundException(s.getAccountId()));

        long days = Math.max(0, ChronoUnit.DAYS.between(s.getStartDate(), LocalDate.now()));
        BigDecimal nonTermRate = SavingsTerm.DEMAND.getAnnualRate();
        BigDecimal actualInterest = s.getTermMonths() == 0
            ? s.getAccruedInterest()
            : calculator.calculateNonTermInterest(s.getPrincipalAmount(), nonTermRate, days);

        BigDecimal forfeitedInterest = s.getAccruedInterest().subtract(actualInterest).max(BigDecimal.ZERO);
        Money principal = Money.of(s.getPrincipalAmount(), customerAcc.getCurrency());
        Money interestToPay = Money.of(actualInterest, customerAcc.getCurrency());

        // Return principal from Vault
        ledgerService.recordTransfer(
            UUID.randomUUID(), SystemAccounts.SAVINGS_VAULT_ACCOUNT_ID, customerAcc.getId(),
            principal, "Premature withdrawal principal " + s.getSavingsNumber(), false
        );

        // Pay non-term interest if positive
        if (interestToPay.isPositive()) {
            ledgerService.recordTransfer(
                UUID.randomUUID(), SystemAccounts.INTEREST_EXPENSE_ACCOUNT_ID, customerAcc.getId(),
                interestToPay, "Non-term interest for premature withdrawal " + s.getSavingsNumber(), false
            );
        }

        s.setStatus(SavingsStatus.PREMATURE_WITHDRAWN);
        savingsRepository.save(s);

        BigDecimal totalPayout = principal.getAmount().add(actualInterest);
        return new PrematureWithdrawalResult(
            savingsId, s.getPrincipalAmount(), forfeitedInterest, actualInterest, totalPayout,
            "Premature withdrawal completed with demand rate: " + nonTermRate
        );
    }

    private void settleToCustomer(SavingsAccount s, Account customer, Money principal, Money interest) {
        ledgerService.recordTransfer(
            UUID.randomUUID(), SystemAccounts.SAVINGS_VAULT_ACCOUNT_ID, customer.getId(),
            principal, "Maturity principal settlement " + s.getSavingsNumber(), false
        );
        if (interest.isPositive()) {
            ledgerService.recordTransfer(
                UUID.randomUUID(), SystemAccounts.INTEREST_EXPENSE_ACCOUNT_ID, customer.getId(),
                interest, "Maturity interest settlement " + s.getSavingsNumber(), false
            );
        }
        s.setStatus(SavingsStatus.SETTLED);
        savingsRepository.save(s);
    }

    private void rolloverAll(SavingsAccount s, Account customer, Money interest) {
        if (interest.isPositive()) {
            ledgerService.recordTransfer(
                UUID.randomUUID(), SystemAccounts.INTEREST_EXPENSE_ACCOUNT_ID, SystemAccounts.SAVINGS_VAULT_ACCOUNT_ID,
                interest, "Rollover interest to savings vault " + s.getSavingsNumber(), false
            );
        }
        s.setPrincipalAmount(s.getPrincipalAmount().add(interest.getAmount()));
        s.setAccruedInterest(BigDecimal.ZERO);
        s.setStartDate(s.getMaturityDate());
        s.setMaturityDate(s.getStartDate().plusMonths(s.getTermMonths()));
        savingsRepository.save(s);
    }

    private void rolloverPrincipalOnly(SavingsAccount s, Account customer, Money interest) {
        if (interest.isPositive()) {
            ledgerService.recordTransfer(
                UUID.randomUUID(), SystemAccounts.INTEREST_EXPENSE_ACCOUNT_ID, customer.getId(),
                interest, "Rollover interest payout " + s.getSavingsNumber(), false
            );
        }
        s.setAccruedInterest(BigDecimal.ZERO);
        s.setStartDate(s.getMaturityDate());
        s.setMaturityDate(s.getStartDate().plusMonths(s.getTermMonths()));
        savingsRepository.save(s);
    }
}
