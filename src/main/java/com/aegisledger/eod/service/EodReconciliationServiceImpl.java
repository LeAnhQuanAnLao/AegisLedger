package com.aegisledger.eod.service;

import com.aegisledger.account.domain.Account;
import com.aegisledger.account.repository.AccountRepository;
import com.aegisledger.eod.domain.DailyAccountingBalanceSheet;
import com.aegisledger.eod.domain.ReconciliationStatus;
import com.aegisledger.eod.repository.DailyBalanceSheetRepository;
import com.aegisledger.ledger.domain.EntryType;
import com.aegisledger.ledger.repository.LedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class EodReconciliationServiceImpl implements EodReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(EodReconciliationServiceImpl.class);

    private final AccountRepository accountRepository;
    private final LedgerRepository ledgerRepository;
    private final DailyBalanceSheetRepository balanceSheetRepository;

    public EodReconciliationServiceImpl(
        AccountRepository accountRepository,
        LedgerRepository ledgerRepository,
        DailyBalanceSheetRepository balanceSheetRepository
    ) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
        this.balanceSheetRepository = balanceSheetRepository;
    }

    @Override
    @Transactional
    public DailyAccountingBalanceSheet runReconciliation(LocalDate reconciliationDate) {
        Objects.requireNonNull(reconciliationDate, "Reconciliation date cannot be null");
        long startMs = System.currentTimeMillis();
        log.info("Starting End-Of-Day reconciliation for date: {}", reconciliationDate);

        // 1. Check System-wide Double-Entry Ledger Equilibrium
        BigDecimal totalDebits = ledgerRepository.sumTotalByEntryType(EntryType.DEBIT);
        BigDecimal totalCredits = ledgerRepository.sumTotalByEntryType(EntryType.CREDIT);
        boolean ledgerBalanced = totalDebits.compareTo(totalCredits) == 0;

        // 2. Audit All Accounts & Invariants
        List<Account> accounts = accountRepository.findAll();
        BigDecimal totalAccountBalance = BigDecimal.ZERO;
        BigDecimal totalLockedBalance = BigDecimal.ZERO;
        BigDecimal totalAvailableBalance = BigDecimal.ZERO;
        int discrepancyCount = 0;
        StringBuilder discrepancies = new StringBuilder();

        for (Account acc : accounts) {
            totalAccountBalance = totalAccountBalance.add(acc.getBalance());
            totalLockedBalance = totalLockedBalance.add(acc.getLockedBalance());
            totalAvailableBalance = totalAvailableBalance.add(acc.getAvailableBalance());

            // Invariant check: balance = availableBalance + lockedBalance
            BigDecimal expectedTotal = acc.getAvailableBalance().add(acc.getLockedBalance());
            if (acc.getBalance().compareTo(expectedTotal) != 0) {
                discrepancyCount++;
                discrepancies.append(String.format("Account %s internal balance mismatch: balance=%s, expected=%s; ",
                    acc.getAccountNumber(), acc.getBalance(), expectedTotal));
            }
        }

        ReconciliationStatus status = (ledgerBalanced && discrepancyCount == 0)
            ? ReconciliationStatus.BALANCED
            : ReconciliationStatus.DISCREPANCY_DETECTED;

        long duration = System.currentTimeMillis() - startMs;

        // 3. Upsert Daily Accounting Balance Sheet
        DailyAccountingBalanceSheet sheet = balanceSheetRepository.findByReconciliationDate(reconciliationDate)
            .orElseGet(DailyAccountingBalanceSheet::new);

        if (sheet.getId() == null) {
            sheet.setId(UUID.randomUUID());
            sheet.setReconciliationDate(reconciliationDate);
            sheet.setCreatedAt(java.time.Instant.now());
        }

        sheet.setTotalAccountsChecked(accounts.size());
        sheet.setTotalAccountBalance(totalAccountBalance);
        sheet.setTotalLockedBalance(totalLockedBalance);
        sheet.setTotalAvailableBalance(totalAvailableBalance);
        sheet.setTotalLedgerDebits(totalDebits);
        sheet.setTotalLedgerCredits(totalCredits);
        sheet.setLedgerBalanced(ledgerBalanced);
        sheet.setDiscrepancyCount(discrepancyCount);
        sheet.setDiscrepanciesDetail(discrepancies.toString());
        sheet.setStatus(status);
        sheet.setExecutionDurationMs(duration);

        DailyAccountingBalanceSheet saved = balanceSheetRepository.save(sheet);
        log.info("Finished EOD reconciliation for {}: status={}, duration={}ms, balanced={}",
            reconciliationDate, status, duration, ledgerBalanced);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DailyAccountingBalanceSheet> getReportByDate(LocalDate date) {
        return balanceSheetRepository.findByReconciliationDate(date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyAccountingBalanceSheet> getRecentReports() {
        return balanceSheetRepository.findTop30ByOrderByReconciliationDateDesc();
    }
}
