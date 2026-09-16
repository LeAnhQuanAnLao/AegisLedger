package com.aegisledger.simulation.service;

import com.aegisledger.account.domain.Account;
import com.aegisledger.account.domain.AccountStatus;
import com.aegisledger.account.repository.AccountRepository;
import com.aegisledger.core.domain.Currency;
import com.aegisledger.core.domain.SystemAccounts;
import com.aegisledger.feelimit.domain.DailyLimitConfig;
import com.aegisledger.ledger.domain.EntryType;
import com.aegisledger.ledger.domain.LedgerEntry;
import com.aegisledger.payment.domain.SagaStep;
import com.aegisledger.payment.domain.Transaction;
import com.aegisledger.payment.domain.TransactionStatus;
import com.aegisledger.simulation.domain.AccountBalanceUpdate;
import com.aegisledger.simulation.repository.SimulationBatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * High-performance service for seeding initial user population.
 */
@Service
public class UserSeedServiceImpl implements UserSeedService {

    private static final Logger log = LoggerFactory.getLogger(UserSeedServiceImpl.class);
    private static final BigDecimal DEFAULT_LIMIT = new BigDecimal("5000.0000");

    private final AccountRepository accountRepository;
    private final SimulationBatchRepository batchRepository;

    public UserSeedServiceImpl(AccountRepository accountRepository, SimulationBatchRepository batchRepository) {
        this.accountRepository = accountRepository;
        this.batchRepository = batchRepository;
    }

    @Override
    @Transactional
    public List<Account> seedUsers(int count, boolean seedInitialDeposit) {
        log.info("Starting bulk seed for {} simulated users...", count);
        Instant now = Instant.now();
        List<Account> accounts = new ArrayList<>(count);
        List<DailyLimitConfig> configs = new ArrayList<>(count);

        Account treasury = accountRepository.findById(SystemAccounts.TREASURY_ACCOUNT_ID)
            .orElseGet(() -> createTreasuryAccount(now));

        BigDecimal treasuryBalance = treasury.getBalance();
        List<Transaction> fundingTxs = new ArrayList<>(count);
        List<LedgerEntry> fundingLedgers = new ArrayList<>(count * 2);

        for (int i = 1; i <= count; i++) {
            UUID accId = UUID.randomUUID();
            String accNumber = String.format("ACC-SIM-%05d", i);
            BigDecimal deposit = determineDepositAmount(i);

            Account acc = createAccountEntity(accId, accNumber, i, deposit, now);
            accounts.add(acc);
            configs.add(new DailyLimitConfig(UUID.randomUUID(), accId, DEFAULT_LIMIT));

            if (seedInitialDeposit && deposit.compareTo(BigDecimal.ZERO) > 0) {
                treasuryBalance = treasuryBalance.subtract(deposit);
                buildFundingEntries(treasury.getId(), acc, deposit, now, fundingTxs, fundingLedgers, treasuryBalance);
            }
        }

        persistSeededData(accounts, configs, fundingTxs, fundingLedgers, treasury.getId(), treasuryBalance);
        log.info("Successfully seeded {} users. Treasury remaining balance: {}", count, treasuryBalance);
        return accounts;
    }

    private BigDecimal determineDepositAmount(int index) {
        int mod = index % 10;
        if (mod < 6) return new BigDecimal("100.0000");
        if (mod < 9) return new BigDecimal("300.0000");
        return new BigDecimal("1000.0000");
    }

    private Account createAccountEntity(UUID id, String number, int index, BigDecimal deposit, Instant now) {
        Account acc = new Account(id, number, "Simulated Customer " + index, deposit, BigDecimal.ZERO,
            deposit, Currency.USD, AccountStatus.ACTIVE);
        acc.setCreatedAt(now);
        acc.setUpdatedAt(now);
        acc.setVersion(0L);
        return acc;
    }

    private void buildFundingEntries(UUID treasuryId, Account acc, BigDecimal deposit, Instant now,
                                     List<Transaction> txs, List<LedgerEntry> ledgers, BigDecimal treasuryBal) {
        UUID txId = UUID.randomUUID();
        Transaction tx = new Transaction(txId, "INIT-DEP-" + acc.getAccountNumber(), treasuryId, acc.getId(),
            deposit, Currency.USD, TransactionStatus.COMPLETED, SagaStep.COMMITTED, null);
        tx.setCreatedAt(now);
        tx.setUpdatedAt(now);
        tx.setVersion(0L);
        txs.add(tx);

        ledgers.add(new LedgerEntry(UUID.randomUUID(), txId, treasuryId, EntryType.DEBIT,
            deposit, treasuryBal, "Initial Treasury Funding"));
        ledgers.add(new LedgerEntry(UUID.randomUUID(), txId, acc.getId(), EntryType.CREDIT,
            deposit, acc.getBalance(), "Initial Account Deposit"));
    }

    private void persistSeededData(List<Account> accounts, List<DailyLimitConfig> configs,
                                   List<Transaction> txs, List<LedgerEntry> ledgers,
                                   UUID treasuryId, BigDecimal finalTreasuryBal) {
        batchRepository.batchInsertAccounts(accounts);
        batchRepository.batchInsertDailyLimitConfigs(configs);
        if (!txs.isEmpty()) {
            batchRepository.batchInsertTransactions(txs);
            batchRepository.batchInsertLedgerEntries(ledgers);
            batchRepository.batchUpdateAccountBalances(List.of(
                new AccountBalanceUpdate(treasuryId, finalTreasuryBal, BigDecimal.ZERO, finalTreasuryBal)
            ));
        }
    }

    private Account createTreasuryAccount(Instant now) {
        Account t = new Account(SystemAccounts.TREASURY_ACCOUNT_ID, SystemAccounts.TREASURY_ACCOUNT_NUMBER,
            "System Bank Treasury", new BigDecimal("10000000.0000"), BigDecimal.ZERO,
            new BigDecimal("10000000.0000"), Currency.USD, AccountStatus.ACTIVE);
        t.setCreatedAt(now);
        t.setUpdatedAt(now);
        t.setVersion(0L);
        return accountRepository.save(t);
    }
}
