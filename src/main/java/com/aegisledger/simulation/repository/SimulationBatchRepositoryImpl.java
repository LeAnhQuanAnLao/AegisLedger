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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * High-performance JDBC batch persistence for bank simulations.
 */
@Repository
public class SimulationBatchRepositoryImpl implements SimulationBatchRepository {

    private static final int BATCH_CHUNK = 2000;
    private final JdbcTemplate jdbcTemplate;

    public SimulationBatchRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private Timestamp safeTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : Timestamp.from(Instant.now());
    }

    @Override
    public void batchInsertAccounts(List<Account> accounts) {
        String sql = "INSERT INTO accounts (id, account_number, holder_name, balance, locked_balance, " +
            "available_balance, currency, status, created_at, updated_at, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, accounts, BATCH_CHUNK, (ps, acc) -> {
            ps.setObject(1, acc.getId());
            ps.setString(2, acc.getAccountNumber());
            ps.setString(3, acc.getHolderName());
            ps.setBigDecimal(4, acc.getBalance());
            ps.setBigDecimal(5, acc.getLockedBalance());
            ps.setBigDecimal(6, acc.getAvailableBalance());
            ps.setString(7, acc.getCurrency().getCode());
            ps.setString(8, acc.getStatus().name());
            ps.setTimestamp(9, safeTimestamp(acc.getCreatedAt()));
            ps.setTimestamp(10, safeTimestamp(acc.getUpdatedAt()));
            ps.setLong(11, acc.getVersion() != null ? acc.getVersion() : 0L);
        });
    }

    @Override
    public void batchInsertDailyLimitConfigs(List<DailyLimitConfig> configs) {
        String sql = "INSERT INTO daily_limit_configs (id, account_id, daily_limit, created_at, updated_at, version) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, configs, BATCH_CHUNK, (ps, cfg) -> {
            ps.setObject(1, cfg.getId());
            ps.setObject(2, cfg.getAccountId());
            ps.setBigDecimal(3, cfg.getDailyLimit());
            ps.setTimestamp(4, safeTimestamp(cfg.getCreatedAt()));
            ps.setTimestamp(5, safeTimestamp(cfg.getUpdatedAt()));
            ps.setLong(6, cfg.getVersion() != null ? cfg.getVersion() : 0L);
        });
    }

    @Override
    public void batchInsertTransactions(List<Transaction> transactions) {
        String sql = "INSERT INTO transactions (id, idempotency_key, source_account_id, destination_account_id, " +
            "amount, currency, status, saga_step, failure_reason, created_at, updated_at, version) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, transactions, BATCH_CHUNK, (ps, tx) -> {
            ps.setObject(1, tx.getId());
            ps.setString(2, tx.getIdempotencyKey());
            ps.setObject(3, tx.getSourceAccountId());
            ps.setObject(4, tx.getDestinationAccountId());
            ps.setBigDecimal(5, tx.getAmount());
            ps.setString(6, tx.getCurrency().getCode());
            ps.setString(7, tx.getStatus().name());
            ps.setString(8, tx.getSagaStep().name());
            ps.setString(9, tx.getFailureReason());
            ps.setTimestamp(10, safeTimestamp(tx.getCreatedAt()));
            ps.setTimestamp(11, safeTimestamp(tx.getUpdatedAt()));
            ps.setLong(12, tx.getVersion() != null ? tx.getVersion() : 0L);
        });
    }

    @Override
    public void batchInsertLedgerEntries(List<LedgerEntry> entries) {
        String sql = "INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after, description, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, entries, BATCH_CHUNK, (ps, le) -> {
            ps.setObject(1, le.getId());
            ps.setObject(2, le.getTransactionId());
            ps.setObject(3, le.getAccountId());
            ps.setString(4, le.getEntryType().name());
            ps.setBigDecimal(5, le.getAmount());
            ps.setBigDecimal(6, le.getBalanceAfter());
            ps.setString(7, le.getDescription());
            ps.setTimestamp(8, safeTimestamp(le.getCreatedAt()));
        });
    }

    @Override
    public void batchUpdateAccountBalances(List<AccountBalanceUpdate> updates) {
        String sql = "UPDATE accounts SET balance = ?, locked_balance = ?, available_balance = ?, updated_at = ?, version = version + 1 WHERE id = ?";
        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.batchUpdate(sql, updates, BATCH_CHUNK, (ps, u) -> {
            ps.setBigDecimal(1, u.balance());
            ps.setBigDecimal(2, u.lockedBalance());
            ps.setBigDecimal(3, u.availableBalance());
            ps.setTimestamp(4, now);
            ps.setObject(5, u.accountId());
        });
    }

    @Override
    public void batchInsertSavingsAccounts(List<SavingsAccount> savingsList) {
        String sql = "INSERT INTO savings_accounts (id, account_id, savings_number, principal_amount, interest_rate, " +
            "term_months, rollover_option, accrued_interest, start_date, maturity_date, last_accrual_date, status, created_at, updated_at, version) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, savingsList, BATCH_CHUNK, (ps, sa) -> {
            ps.setObject(1, sa.getId());
            ps.setObject(2, sa.getAccountId());
            ps.setString(3, sa.getSavingsNumber());
            ps.setBigDecimal(4, sa.getPrincipalAmount());
            ps.setBigDecimal(5, sa.getInterestRate());
            ps.setInt(6, sa.getTermMonths());
            ps.setString(7, sa.getRolloverOption().name());
            ps.setBigDecimal(8, sa.getAccruedInterest());
            ps.setDate(9, Date.valueOf(sa.getStartDate()));
            ps.setDate(10, sa.getMaturityDate() != null ? Date.valueOf(sa.getMaturityDate()) : null);
            ps.setDate(11, sa.getLastAccrualDate() != null ? Date.valueOf(sa.getLastAccrualDate()) : null);
            ps.setString(12, sa.getStatus().name());
            ps.setTimestamp(13, safeTimestamp(sa.getCreatedAt()));
            ps.setTimestamp(14, safeTimestamp(sa.getUpdatedAt()));
            ps.setLong(15, sa.getVersion() != null ? sa.getVersion() : 0L);
        });
    }

    @Override
    public void batchUpdateSavingsAccrual(List<SavingsAccount> savingsList) {
        String sql = "UPDATE savings_accounts SET accrued_interest = ?, last_accrual_date = ?, status = ?, updated_at = ? WHERE id = ?";
        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.batchUpdate(sql, savingsList, BATCH_CHUNK, (ps, sa) -> {
            ps.setBigDecimal(1, sa.getAccruedInterest());
            ps.setDate(2, sa.getLastAccrualDate() != null ? Date.valueOf(sa.getLastAccrualDate()) : null);
            ps.setString(3, sa.getStatus().name());
            ps.setTimestamp(4, now);
            ps.setObject(5, sa.getId());
        });
    }

    @Override
    public void batchInsertLoanContracts(List<LoanContract> loans) {
        String sql = "INSERT INTO loan_contracts (id, account_id, loan_number, principal_amount, interest_rate, " +
            "term_months, remaining_principal, status, disbursed_at, created_at, updated_at, version) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, loans, BATCH_CHUNK, (ps, l) -> {
            ps.setObject(1, l.getId());
            ps.setObject(2, l.getAccountId());
            ps.setString(3, l.getLoanNumber());
            ps.setBigDecimal(4, l.getPrincipalAmount());
            ps.setBigDecimal(5, l.getInterestRate());
            ps.setInt(6, l.getTermMonths());
            ps.setBigDecimal(7, l.getRemainingPrincipal());
            ps.setString(8, l.getStatus().name());
            ps.setTimestamp(9, l.getDisbursedAt() != null ? Timestamp.from(l.getDisbursedAt()) : null);
            ps.setTimestamp(10, safeTimestamp(l.getCreatedAt()));
            ps.setTimestamp(11, safeTimestamp(l.getUpdatedAt()));
            ps.setLong(12, l.getVersion() != null ? l.getVersion() : 0L);
        });
    }

    @Override
    public void batchInsertLoanSchedules(List<LoanRepaymentSchedule> schedules) {
        String sql = "INSERT INTO loan_repayment_schedules (id, loan_id, installment_number, due_date, " +
            "principal_due, interest_due, total_due, principal_paid, interest_paid, status, paid_at, created_at, updated_at, version) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, schedules, BATCH_CHUNK, (ps, s) -> {
            ps.setObject(1, s.getId());
            ps.setObject(2, s.getLoanId());
            ps.setInt(3, s.getInstallmentNumber());
            ps.setDate(4, Date.valueOf(s.getDueDate()));
            ps.setBigDecimal(5, s.getPrincipalDue());
            ps.setBigDecimal(6, s.getInterestDue());
            ps.setBigDecimal(7, s.getTotalDue());
            ps.setBigDecimal(8, s.getPrincipalPaid());
            ps.setBigDecimal(9, s.getInterestPaid());
            ps.setString(10, s.getStatus().name());
            ps.setTimestamp(11, s.getPaidAt() != null ? Timestamp.from(s.getPaidAt()) : null);
            ps.setTimestamp(12, safeTimestamp(s.getCreatedAt()));
            ps.setTimestamp(13, safeTimestamp(s.getUpdatedAt()));
            ps.setLong(14, s.getVersion() != null ? s.getVersion() : 0L);
        });
    }

    @Override
    public void batchUpdateLoanSchedules(List<LoanRepaymentSchedule> schedules) {
        String sql = "UPDATE loan_repayment_schedules SET principal_paid = ?, interest_paid = ?, status = ?, paid_at = ?, updated_at = ? WHERE id = ?";
        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.batchUpdate(sql, schedules, BATCH_CHUNK, (ps, s) -> {
            ps.setBigDecimal(1, s.getPrincipalPaid());
            ps.setBigDecimal(2, s.getInterestPaid());
            ps.setString(3, s.getStatus().name());
            ps.setTimestamp(4, s.getPaidAt() != null ? Timestamp.from(s.getPaidAt()) : null);
            ps.setTimestamp(5, now);
            ps.setObject(6, s.getId());
        });
    }

    @Override
    public void batchInsertOrUpdateDailyLimitUsages(List<DailyLimitUsage> usages) {
        String sql = "INSERT INTO daily_limit_usages (id, account_id, usage_date, total_spent, created_at, updated_at, version) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, usages, BATCH_CHUNK, (ps, u) -> {
            ps.setObject(1, u.getId());
            ps.setObject(2, u.getAccountId());
            ps.setDate(3, Date.valueOf(u.getUsageDate()));
            ps.setBigDecimal(4, u.getTotalSpent());
            ps.setTimestamp(5, safeTimestamp(u.getCreatedAt()));
            ps.setTimestamp(6, safeTimestamp(u.getUpdatedAt()));
            ps.setLong(7, u.getVersion() != null ? u.getVersion() : 0L);
        });
    }
}
