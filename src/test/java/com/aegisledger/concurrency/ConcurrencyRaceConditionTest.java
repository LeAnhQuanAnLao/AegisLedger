package com.aegisledger.concurrency;

import com.aegisledger.account.domain.Account;
import com.aegisledger.account.repository.AccountRepository;
import com.aegisledger.core.domain.Currency;
import com.aegisledger.core.domain.Money;
import com.aegisledger.ledger.service.DoubleEntryLedgerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("High-Concurrency Stress Test: Zero Race Condition & Invariant Balance")
class ConcurrencyRaceConditionTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private DoubleEntryLedgerService ledgerService;

    @Test
    @DisplayName("50 concurrent threads transferring A->B and 50 threads B->A must maintain zero race condition and conserve total balance")
    void testHighConcurrencyTransfersPreserveSystemBalance() throws InterruptedException {
        // Arrange
        UUID accountAId = UUID.randomUUID();
        UUID accountBId = UUID.randomUUID();

        Account accountA = new Account(accountAId, "ACC-CONC-A", "Alice", Money.of(10000.00, Currency.USD));
        Account accountB = new Account(accountBId, "ACC-CONC-B", "Bob", Money.of(10000.00, Currency.USD));

        accountRepository.save(accountA);
        accountRepository.save(accountB);

        BigDecimal initialTotal = accountA.getBalance().add(accountB.getBalance());
        assertEquals(new BigDecimal("20000.0000"), initialTotal);

        int totalThreads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch latch = new CountDownLatch(totalThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Money transferAmount = Money.of(10.00, Currency.USD);

        // Act: 50 threads transfer A -> B, 50 threads transfer B -> A concurrently
        for (int i = 0; i < totalThreads; i++) {
            final boolean direction = (i % 2 == 0);
            executor.submit(() -> {
                try {
                    UUID src = direction ? accountAId : accountBId;
                    UUID dst = direction ? accountBId : accountAId;
                    ledgerService.recordTransfer(UUID.randomUUID(), src, dst, transferAmount, "Concurrent transfer", false);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(completed, "Concurrency test timed out");

        // Assert
        Account refreshedA = accountRepository.findById(accountAId).orElseThrow();
        Account refreshedB = accountRepository.findById(accountBId).orElseThrow();

        BigDecimal finalTotal = refreshedA.getBalance().add(refreshedB.getBalance());

        // Zero Race Condition: Balance cannot be negative
        assertTrue(refreshedA.getBalance().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(refreshedB.getBalance().compareTo(BigDecimal.ZERO) >= 0);

        // Absolute Financial Conservation Invariant: Sum of system balances must be identical to initial total!
        assertEquals(
            initialTotal,
            finalTotal,
            "Financial invariant violated! Total system money was altered under concurrency"
        );
        assertEquals(totalThreads, successCount.get());
        assertEquals(0, failureCount.get());
    }
}
