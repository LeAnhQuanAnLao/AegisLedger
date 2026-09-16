package com.aegisledger.simulation.repository;

import com.aegisledger.account.domain.Account;
import com.aegisledger.feelimit.domain.DailyLimitConfig;
import com.aegisledger.feelimit.domain.DailyLimitUsage;
import com.aegisledger.ledger.domain.LedgerEntry;
import com.aegisledger.lending.domain.LoanContract;
import com.aegisledger.lending.domain.LoanRepaymentSchedule;
import com.aegisledger.payment.domain.Transaction;
import com.aegisledger.savings.domain.SavingsAccount;
import com.aegisledger.simulation.domain.AccountBalanceUpdate;

import java.util.List;

/**
 * High-throughput batch operations for database simulation writes.
 */
public interface SimulationBatchRepository {

    void batchInsertAccounts(List<Account> accounts);

    void batchInsertDailyLimitConfigs(List<DailyLimitConfig> configs);

    void batchInsertTransactions(List<Transaction> transactions);

    void batchInsertLedgerEntries(List<LedgerEntry> entries);

    void batchUpdateAccountBalances(List<AccountBalanceUpdate> updates);

    void batchInsertSavingsAccounts(List<SavingsAccount> savingsList);

    void batchUpdateSavingsAccrual(List<SavingsAccount> savingsList);

    void batchInsertLoanContracts(List<LoanContract> loans);

    void batchInsertLoanSchedules(List<LoanRepaymentSchedule> schedules);

    void batchUpdateLoanSchedules(List<LoanRepaymentSchedule> schedules);

    void batchInsertOrUpdateDailyLimitUsages(List<DailyLimitUsage> usages);
}
