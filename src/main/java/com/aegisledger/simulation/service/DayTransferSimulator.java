package com.aegisledger.simulation.service;

import com.aegisledger.account.domain.Account;
import com.aegisledger.core.domain.Currency;
import com.aegisledger.core.domain.SystemAccounts;
import com.aegisledger.feelimit.domain.DailyLimitUsage;
import com.aegisledger.ledger.domain.EntryType;
import com.aegisledger.ledger.domain.LedgerEntry;
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
 * Sub-simulator for executing daily peer-to-peer transfers with fees and double-entry book balancing.
 */
@Component
public class DayTransferSimulator {

    private static final BigDecimal FEE = new BigDecimal("0.5000");
    private final Random random = new Random();

    public record TransferBatchResult(
        List<Transaction> transactions,
        List<LedgerEntry> ledgerEntries,
        List<DailyLimitUsage> limitUsages,
        BigDecimal totalVolume,
        BigDecimal totalFees,
        Set<UUID> modifiedAccountIds
    ) {}

    public TransferBatchResult simulateTransfers(
        LocalDate date,
        int count,
        List<Account> accounts,
        Map<UUID, Account> lookup,
        Account feeRevAccount
    ) {
        Instant now = Instant.now();
        List<Transaction> txs = new ArrayList<>(count);
        List<LedgerEntry> ledgers = new ArrayList<>(count * 4);
        Map<UUID, BigDecimal> dailySpentMap = new HashMap<>();
        Set<UUID> modified = new HashSet<>();

        BigDecimal totalVolume = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;
        int totalAccounts = accounts.size();

        for (int i = 0; i < count; i++) {
            int srcIdx = random.nextInt(totalAccounts);
            int dstIdx = random.nextInt(totalAccounts);
            if (srcIdx == dstIdx) continue;

            Account src = accounts.get(srcIdx);
            Account dst = accounts.get(dstIdx);
            BigDecimal amount = BigDecimal.valueOf(10 + random.nextInt(40)).setScale(4, RoundingMode.HALF_EVEN);
            BigDecimal required = amount.add(FEE);

            if (src.getAvailableBalance().compareTo(required) < 0) continue;

            applyBalances(src, dst, feeRevAccount, amount, FEE);
            UUID txId = UUID.randomUUID();
            Transaction tx = new Transaction(txId, "SIM-TX-" + UUID.randomUUID(), src.getId(), dst.getId(),
                amount, Currency.USD, TransactionStatus.COMPLETED, SagaStep.COMMITTED, null);
            tx.setCreatedAt(now);
            tx.setUpdatedAt(now);
            tx.setVersion(0L);
            txs.add(tx);

            createLedgerEntries(ledgers, txId, src, dst, feeRevAccount, amount, FEE, now);
            dailySpentMap.merge(src.getId(), amount, BigDecimal::add);

            modified.add(src.getId());
            modified.add(dst.getId());
            modified.add(feeRevAccount.getId());
            totalVolume = totalVolume.add(amount);
            totalFees = totalFees.add(FEE);
        }

        List<DailyLimitUsage> limitUsages = toLimitUsages(dailySpentMap, date, now);
        return new TransferBatchResult(txs, ledgers, limitUsages, totalVolume, totalFees, modified);
    }

    private void applyBalances(Account src, Account dst, Account feeRev, BigDecimal amount, BigDecimal fee) {
        src.setBalance(src.getBalance().subtract(amount.add(fee)));
        src.setAvailableBalance(src.getAvailableBalance().subtract(amount.add(fee)));

        dst.setBalance(dst.getBalance().add(amount));
        dst.setAvailableBalance(dst.getAvailableBalance().add(amount));

        feeRev.setBalance(feeRev.getBalance().add(fee));
        feeRev.setAvailableBalance(feeRev.getAvailableBalance().add(fee));
    }

    private void createLedgerEntries(List<LedgerEntry> ledgers, UUID txId, Account src, Account dst,
                                     Account feeRev, BigDecimal amount, BigDecimal fee, Instant now) {
        ledgers.add(new LedgerEntry(UUID.randomUUID(), txId, src.getId(), EntryType.DEBIT,
            amount, src.getBalance().add(fee), "P2P Transfer Debit"));
        ledgers.add(new LedgerEntry(UUID.randomUUID(), txId, dst.getId(), EntryType.CREDIT,
            amount, dst.getBalance(), "P2P Transfer Credit"));
        ledgers.add(new LedgerEntry(UUID.randomUUID(), txId, src.getId(), EntryType.DEBIT,
            fee, src.getBalance(), "Transfer Fee Debit"));
        ledgers.add(new LedgerEntry(UUID.randomUUID(), txId, feeRev.getId(), EntryType.CREDIT,
            fee, feeRev.getBalance(), "Transfer Fee Revenue"));
    }

    private List<DailyLimitUsage> toLimitUsages(Map<UUID, BigDecimal> map, LocalDate date, Instant now) {
        List<DailyLimitUsage> list = new ArrayList<>(map.size());
        for (var entry : map.entrySet()) {
            DailyLimitUsage u = new DailyLimitUsage(UUID.randomUUID(), entry.getKey(), date, entry.getValue());
            u.setCreatedAt(now);
            u.setUpdatedAt(now);
            u.setVersion(0L);
            list.add(u);
        }
        return list;
    }
}
