package com.aegisledger.simulation.service;

import com.aegisledger.account.domain.Account;
import com.aegisledger.core.domain.Currency;
import com.aegisledger.core.domain.SystemAccounts;
import com.aegisledger.ledger.domain.EntryType;
import com.aegisledger.ledger.domain.LedgerEntry;
import com.aegisledger.payment.domain.SagaStep;
import com.aegisledger.payment.domain.Transaction;
import com.aegisledger.payment.domain.TransactionStatus;
import com.aegisledger.savings.domain.RolloverOption;
import com.aegisledger.savings.domain.SavingsAccount;
import com.aegisledger.savings.domain.SavingsStatus;
import com.aegisledger.savings.domain.SavingsTerm;
import com.aegisledger.savings.service.SavingsInterestCalculator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Sub-simulator for savings account deposits and daily compound interest accrual.
 */
@Component
public class DaySavingsSimulator {

    private final SavingsInterestCalculator calculator;
    private final Random random = new Random();

    public DaySavingsSimulator(SavingsInterestCalculator calculator) {
        this.calculator = calculator;
    }

    public record SavingsBatchResult(
        List<SavingsAccount> newSavings,
        List<SavingsAccount> updatedSavings,
        List<Transaction> transactions,
        List<LedgerEntry> ledgerEntries,
        BigDecimal totalPrincipal,
        BigDecimal totalAccruedToday,
        Set<UUID> modifiedAccountIds
    ) {}

    public SavingsBatchResult simulateSavings(
        LocalDate date,
        int count,
        List<Account> accounts,
        Map<UUID, Account> lookup,
        Account vaultAccount,
        List<SavingsAccount> activeSavingsPool
    ) {
        Instant now = Instant.now();
        List<SavingsAccount> newSavings = new ArrayList<>(count);
        List<Transaction> txs = new ArrayList<>(count);
        List<LedgerEntry> ledgers = new ArrayList<>(count * 2);
        Set<UUID> modified = new HashSet<>();
        BigDecimal totalPrincipal = BigDecimal.ZERO;

        int totalUsers = accounts.size();
        for (int i = 0; i < count; i++) {
            Account acc = accounts.get(random.nextInt(totalUsers));
            BigDecimal principal = BigDecimal.valueOf(100 + random.nextInt(400)).setScale(4, RoundingMode.HALF_EVEN);
            if (acc.getAvailableBalance().compareTo(principal) < 0) continue;

            SavingsTerm term = pickRandomTerm();
            SavingsAccount sa = openSavings(acc, vaultAccount, term, principal, date, now, txs, ledgers);
            newSavings.add(sa);
            activeSavingsPool.add(sa);
            modified.add(acc.getId());
            modified.add(vaultAccount.getId());
            totalPrincipal = totalPrincipal.add(principal);
        }

        BigDecimal accruedToday = accrueAll(activeSavingsPool, date);
        return new SavingsBatchResult(newSavings, activeSavingsPool, txs, ledgers, totalPrincipal, accruedToday, modified);
    }

    private SavingsAccount openSavings(Account acc, Account vault, SavingsTerm term, BigDecimal principal,
                                       LocalDate date, Instant now, List<Transaction> txs, List<LedgerEntry> ledgers) {
        acc.setBalance(acc.getBalance().subtract(principal));
        acc.setAvailableBalance(acc.getAvailableBalance().subtract(principal));
        vault.setBalance(vault.getBalance().add(principal));
        vault.setAvailableBalance(vault.getAvailableBalance().add(principal));

        UUID txId = UUID.randomUUID();
        Transaction tx = new Transaction(txId, "SAV-DEP-" + UUID.randomUUID(), acc.getId(), vault.getId(),
            principal, Currency.USD, TransactionStatus.COMPLETED, SagaStep.COMMITTED, null);
        tx.setCreatedAt(now);
        tx.setUpdatedAt(now);
        tx.setVersion(0L);
        txs.add(tx);

        ledgers.add(new LedgerEntry(UUID.randomUUID(), txId, acc.getId(), EntryType.DEBIT,
            principal, acc.getBalance(), "Savings Deposit Lock"));
        ledgers.add(new LedgerEntry(UUID.randomUUID(), txId, vault.getId(), EntryType.CREDIT,
            principal, vault.getBalance(), "Savings Vault Credit"));

        SavingsAccount sa = new SavingsAccount(UUID.randomUUID(), acc.getId(),
            "SAV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            principal, term.getAnnualRate(), term.getMonths(), RolloverOption.AUTO_SETTLE, date);
        sa.setCreatedAt(now);
        sa.setUpdatedAt(now);
        sa.setVersion(0L);
        return sa;
    }

    private BigDecimal accrueAll(List<SavingsAccount> pool, LocalDate date) {
        BigDecimal total = BigDecimal.ZERO;
        for (SavingsAccount sa : pool) {
            if (sa.getStatus() == SavingsStatus.ACTIVE) {
                BigDecimal daily = calculator.calculateDailyInterest(sa.getPrincipalAmount(), sa.getInterestRate());
                sa.addAccruedInterest(daily, date);
                total = total.add(daily);
            }
        }
        return total;
    }

    private SavingsTerm pickRandomTerm() {
        SavingsTerm[] terms = SavingsTerm.values();
        return terms[random.nextInt(terms.length)];
    }
}
