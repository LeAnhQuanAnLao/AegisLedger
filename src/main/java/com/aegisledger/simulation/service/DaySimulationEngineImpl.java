package com.aegisledger.simulation.service;

import com.aegisledger.account.domain.Account;
import com.aegisledger.core.domain.SystemAccounts;
import com.aegisledger.eod.dto.EodReportDto;
import com.aegisledger.eod.service.EodReconciliationService;
import com.aegisledger.ledger.domain.LedgerEntry;
import com.aegisledger.lending.domain.LoanRepaymentSchedule;
import com.aegisledger.payment.domain.Transaction;
import com.aegisledger.savings.domain.SavingsAccount;
import com.aegisledger.simulation.domain.AccountBalanceUpdate;
import com.aegisledger.simulation.domain.SimulationConfig;
import com.aegisledger.simulation.dto.DaySimulationReport;
import com.aegisledger.simulation.repository.SimulationBatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Implementation of daily simulation coordinator and batch persistence.
 */
@Service
public class DaySimulationEngineImpl implements DaySimulationEngine {

    private static final Logger log = LoggerFactory.getLogger(DaySimulationEngineImpl.class);

    private final DayTransferSimulator transferSimulator;
    private final DaySavingsSimulator savingsSimulator;
    private final DayLoanSimulator loanSimulator;
    private final SimulationBatchRepository batchRepository;
    private final EodReconciliationService eodService;

    private final List<SavingsAccount> activeSavingsPool = new ArrayList<>();
    private final List<LoanRepaymentSchedule> pendingLoanSchedules = new ArrayList<>();

    public DaySimulationEngineImpl(
        DayTransferSimulator transferSimulator,
        DaySavingsSimulator savingsSimulator,
        DayLoanSimulator loanSimulator,
        SimulationBatchRepository batchRepository,
        EodReconciliationService eodService
    ) {
        this.transferSimulator = transferSimulator;
        this.savingsSimulator = savingsSimulator;
        this.loanSimulator = loanSimulator;
        this.batchRepository = batchRepository;
        this.eodService = eodService;
    }

    @Override
    @Transactional
    public DaySimulationReport simulateDay(
        int dayIndex,
        LocalDate date,
        SimulationConfig config,
        List<Account> accounts,
        Map<UUID, Account> lookup
    ) {
        long startMs = System.currentTimeMillis();
        Account feeRev = lookup.get(SystemAccounts.FEE_REVENUE_ACCOUNT_ID);
        Account vault = lookup.get(SystemAccounts.SAVINGS_VAULT_ACCOUNT_ID);
        Account treasury = lookup.get(SystemAccounts.TREASURY_ACCOUNT_ID);
        Account interestInc = lookup.get(SystemAccounts.INTEREST_INCOME_ACCOUNT_ID);

        var txRes = transferSimulator.simulateTransfers(date, config.dailyTransferCount(), accounts, lookup, feeRev);
        var savRes = savingsSimulator.simulateSavings(date, config.dailySavingsCount(), accounts, lookup, vault, activeSavingsPool);
        var loanRes = loanSimulator.simulateLoans(date, config.dailyLoanCount(), accounts, lookup, treasury, interestInc, pendingLoanSchedules);

        flushDayToDatabase(txRes, savRes, loanRes, lookup);
        var eodSheet = eodService.runReconciliation(date);
        long durationMs = System.currentTimeMillis() - startMs;

        boolean isBalanced = eodSheet.isLedgerBalanced();
        log.info("Simulated Day {} ({}): {} txs, {} ledgers, EOD balanced={}, duration={}ms",
            dayIndex, date, txRes.transactions().size(), txRes.ledgerEntries().size(), isBalanced, durationMs);

        return new DaySimulationReport(dayIndex, date, txRes.transactions().size(), txRes.totalVolume(),
            txRes.totalFees(), savRes.newSavings().size(), savRes.totalPrincipal(), savRes.totalAccruedToday(),
            loanRes.newLoans().size(), loanRes.totalDisbursed(), loanRes.totalRepaid(), isBalanced, durationMs);
    }

    private void flushDayToDatabase(
        DayTransferSimulator.TransferBatchResult txRes,
        DaySavingsSimulator.SavingsBatchResult savRes,
        DayLoanSimulator.LoanBatchResult loanRes,
        Map<UUID, Account> lookup
    ) {
        List<Transaction> allTxs = new ArrayList<>(txRes.transactions());
        allTxs.addAll(savRes.transactions());
        allTxs.addAll(loanRes.transactions());
        if (!allTxs.isEmpty()) batchRepository.batchInsertTransactions(allTxs);

        List<LedgerEntry> allLedgers = new ArrayList<>(txRes.ledgerEntries());
        allLedgers.addAll(savRes.ledgerEntries());
        allLedgers.addAll(loanRes.ledgerEntries());
        if (!allLedgers.isEmpty()) batchRepository.batchInsertLedgerEntries(allLedgers);

        if (!txRes.limitUsages().isEmpty()) {
            batchRepository.batchInsertOrUpdateDailyLimitUsages(txRes.limitUsages());
        }
        if (!savRes.newSavings().isEmpty()) {
            batchRepository.batchInsertSavingsAccounts(savRes.newSavings());
        }
        if (!loanRes.newLoans().isEmpty()) {
            batchRepository.batchInsertLoanContracts(loanRes.newLoans());
            batchRepository.batchInsertLoanSchedules(loanRes.newSchedules());
        }

        Set<UUID> modified = new HashSet<>(txRes.modifiedAccountIds());
        modified.addAll(savRes.modifiedAccountIds());
        modified.addAll(loanRes.modifiedAccountIds());

        List<AccountBalanceUpdate> updates = new ArrayList<>(modified.size());
        for (UUID id : modified) {
            Account acc = lookup.get(id);
            if (acc != null) {
                updates.add(new AccountBalanceUpdate(acc.getId(), acc.getBalance(), acc.getLockedBalance(), acc.getAvailableBalance()));
            }
        }
        if (!updates.isEmpty()) batchRepository.batchUpdateAccountBalances(updates);
    }
}
