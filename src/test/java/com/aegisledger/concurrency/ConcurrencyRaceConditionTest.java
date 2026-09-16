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

    @Autowired
    private com.aegisledger.payment.service.PaymentOrchestratorService paymentService;

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

    @Test
    @DisplayName("50 concurrent threads attempting to withdraw $50 from a $100 account: exactly 2 must succeed, 48 must fail, balance cannot be negative")
    void testConcurrentWithdrawalSingleAccountZeroOverdraw() throws InterruptedException {
        // Arrange
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();

        // Alice only has $100.00
        Account alice = new Account(aliceId, "ACC-OVERDRAW-ALICE", "Alice", Money.of(100.00, Currency.USD));
        Account bob = new Account(bobId, "ACC-OVERDRAW-BOB", "Bob", Money.of(0.00, Currency.USD));

        accountRepository.save(alice);
        accountRepository.save(bob);

        int concurrentThreads = 50;
        Money withdrawAmount = Money.of(50.00, Currency.USD);

        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(concurrentThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger insufficientFundsCount = new AtomicInteger(0);
        AtomicInteger otherErrors = new AtomicInteger(0);

        // Act: 50 threads try to withdraw $50 at the exact same time
        for (int i = 0; i < concurrentThreads; i++) {
            executor.submit(() -> {
                try {
                    startGate.await(); // Wait for all threads to be ready
                    ledgerService.recordTransfer(
                        UUID.randomUUID(),
                        aliceId,
                        bobId,
                        withdrawAmount,
                        "Concurrent withdrawal",
                        false
                    );
                    successCount.incrementAndGet();
                } catch (com.aegisledger.core.exception.InsufficientFundsException e) {
                    insufficientFundsCount.incrementAndGet();
                } catch (Exception e) {
                    otherErrors.incrementAndGet();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        // Fire all threads at once
        startGate.countDown();
        boolean finished = doneGate.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(finished, "Test timed out under concurrency");

        // Assert
        Account refreshedAlice = accountRepository.findById(aliceId).orElseThrow();
        Account refreshedBob = accountRepository.findById(bobId).orElseThrow();

        // Exactly 2 withdrawals of $50 must succeed from a $100 balance
        assertEquals(2, successCount.get(), "Exactly 2 withdrawals should succeed");
        assertEquals(48, insufficientFundsCount.get(), "Remaining 48 attempts must fail with InsufficientFundsException");
        assertEquals(0, otherErrors.get(), "No unexpected errors or deadlocks allowed");

        // Account balances must be exact and never negative
        assertEquals(new BigDecimal("0.0000"), refreshedAlice.getBalance(), "Alice balance should be exactly 0.0000");
        assertEquals(new BigDecimal("100.0000"), refreshedBob.getBalance(), "Bob balance should be exactly 100.0000");

        // Financial invariant: Total money preserved
        BigDecimal total = refreshedAlice.getBalance().add(refreshedBob.getBalance());
        assertEquals(new BigDecimal("100.0000"), total, "Total money in system must remain exactly $100.00");
    }

    @Test
    @DisplayName("1,000 concurrent threads transferring A->B and B->A simultaneously: zero race condition, zero deadlock, total balance conserved")
    void testOneThousandConcurrentTransfersPreserveSystemBalance() throws InterruptedException {
        // Arrange
        UUID accountAId = UUID.randomUUID();
        UUID accountBId = UUID.randomUUID();

        Account accountA = new Account(accountAId, "ACC-CONC-1K-A", "Alice 1K", Money.of(100000.00, Currency.USD));
        Account accountB = new Account(accountBId, "ACC-CONC-1K-B", "Bob 1K", Money.of(100000.00, Currency.USD));

        accountRepository.save(accountA);
        accountRepository.save(accountB);

        BigDecimal initialTotal = accountA.getBalance().add(accountB.getBalance());
        assertEquals(new BigDecimal("200000.0000"), initialTotal);

        int totalThreads = 1000;
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(totalThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Money transferAmount = Money.of(10.00, Currency.USD);

        // Act: 500 threads transfer A -> B, 500 threads transfer B -> A concurrently
        for (int i = 0; i < totalThreads; i++) {
            final boolean direction = (i % 2 == 0);
            executor.submit(() -> {
                try {
                    startGate.await();
                    UUID src = direction ? accountAId : accountBId;
                    UUID dst = direction ? accountBId : accountAId;
                    ledgerService.recordTransfer(UUID.randomUUID(), src, dst, transferAmount, "1K Concurrent transfer", false);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown(); // Release all 1,000 threads simultaneously!
        boolean completed = doneGate.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(completed, "1,000 concurrency test timed out");

        // Assert
        Account refreshedA = accountRepository.findById(accountAId).orElseThrow();
        Account refreshedB = accountRepository.findById(accountBId).orElseThrow();

        BigDecimal finalTotal = refreshedA.getBalance().add(refreshedB.getBalance());

        assertTrue(refreshedA.getBalance().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(refreshedB.getBalance().compareTo(BigDecimal.ZERO) >= 0);

        assertEquals(
            initialTotal,
            finalTotal,
            "Financial invariant violated under 1,000 concurrent threads!"
        );
        assertEquals(totalThreads, successCount.get(), "All 1,000 transfers must complete successfully");
        assertEquals(0, failureCount.get(), "Zero failures or deadlocks allowed");
    }

    @Test
    @DisplayName("1,000 concurrent threads attempting to withdraw $10 from a $1,000 account: exactly 100 succeed, 900 fail, balance cannot be negative")
    void testOneThousandConcurrentWithdrawalsFromSingleAccount() throws InterruptedException {
        // Arrange
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();

        // Alice only has $1,000.00
        Account alice = new Account(aliceId, "ACC-OVERDRAW-1K-ALICE", "Alice HighContention", Money.of(1000.00, Currency.USD));
        Account bob = new Account(bobId, "ACC-OVERDRAW-1K-BOB", "Bob Merchant", Money.of(0.00, Currency.USD));

        accountRepository.save(alice);
        accountRepository.save(bob);

        int concurrentThreads = 1000;
        Money withdrawAmount = Money.of(10.00, Currency.USD);

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(concurrentThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger insufficientFundsCount = new AtomicInteger(0);
        AtomicInteger otherErrors = new AtomicInteger(0);

        // Act: 1,000 threads try to withdraw $10 at the exact same time
        for (int i = 0; i < concurrentThreads; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    ledgerService.recordTransfer(
                        UUID.randomUUID(),
                        aliceId,
                        bobId,
                        withdrawAmount,
                        "1K Concurrent withdrawal",
                        false
                    );
                    successCount.incrementAndGet();
                } catch (com.aegisledger.core.exception.InsufficientFundsException e) {
                    insufficientFundsCount.incrementAndGet();
                } catch (Exception e) {
                    otherErrors.incrementAndGet();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown(); // Fire 1,000 threads all at once!
        boolean finished = doneGate.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(finished, "Test timed out under 1,000 threads concurrency");

        // Assert
        Account refreshedAlice = accountRepository.findById(aliceId).orElseThrow();
        Account refreshedBob = accountRepository.findById(bobId).orElseThrow();

        // Exactly 100 withdrawals of $10 must succeed from a $1,000 balance
        assertEquals(100, successCount.get(), "Exactly 100 withdrawals should succeed ($1,000 / $10 = 100)");
        assertEquals(900, insufficientFundsCount.get(), "Remaining 900 attempts must fail with InsufficientFundsException");
        assertEquals(0, otherErrors.get(), "No unexpected errors or deadlocks allowed under 1,000 threads");

        // Account balances must be exact and never negative
        assertEquals(new BigDecimal("0.0000"), refreshedAlice.getBalance(), "Alice balance must be exactly 0.0000");
        assertEquals(new BigDecimal("1000.0000"), refreshedBob.getBalance(), "Bob balance must be exactly 1000.0000");

        // Financial invariant: Total money preserved
        BigDecimal total = refreshedAlice.getBalance().add(refreshedBob.getBalance());
        assertEquals(new BigDecimal("1000.0000"), total, "Total money in system must remain exactly $1,000.00");
    }

    @Test
    @DisplayName("Concurrent bidirectional transfers (A->B and B->A) via Saga: Zero Deadlock, all succeed and balance preserved")
    void testConcurrentReverseSagaTransfersZeroDeadlock() throws InterruptedException {
        // Arrange: Create 5 pairs of accounts
        int pairCount = 5;
        UUID[] aliceIds = new UUID[pairCount];
        UUID[] bobIds = new UUID[pairCount];
        BigDecimal initialPairBalance = new BigDecimal("5000.0000");

        for (int i = 0; i < pairCount; i++) {
            aliceIds[i] = UUID.randomUUID();
            bobIds[i] = UUID.randomUUID();
            accountRepository.save(new Account(aliceIds[i], "SAGA-CONC-A-" + i, "Alice " + i, Money.of(2500.00, Currency.USD)));
            accountRepository.save(new Account(bobIds[i], "SAGA-CONC-B-" + i, "Bob " + i, Money.of(2500.00, Currency.USD)));
        }

        int totalTransfers = pairCount * 2; // 10 transfers (A->B and B->A for each pair)
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(totalTransfers);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Act: For each pair, fire A->B and B->A at the exact same instant
        for (int i = 0; i < pairCount; i++) {
            final UUID aId = aliceIds[i];
            final UUID bId = bobIds[i];
            final int pairIndex = i;

            // Thread 1: A -> B ($50)
            executor.submit(() -> {
                try {
                    startGate.await();
                    paymentService.transfer(new com.aegisledger.payment.dto.TransferRequest(
                        aId, bId, new BigDecimal("50.00"), Currency.USD,
                        "SAGA-CONC-KEY-AB-" + pairIndex + "-" + UUID.randomUUID(), "Transfer A to B"
                    ));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneGate.countDown();
                }
            });

            // Thread 2: B -> A ($30)
            executor.submit(() -> {
                try {
                    startGate.await();
                    paymentService.transfer(new com.aegisledger.payment.dto.TransferRequest(
                        bId, aId, new BigDecimal("30.00"), Currency.USD,
                        "SAGA-CONC-KEY-BA-" + pairIndex + "-" + UUID.randomUUID(), "Transfer B to A"
                    ));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown(); // Fire all concurrent saga transfers simultaneously
        boolean completed = doneGate.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        assertTrue(completed, "Bidirectional Saga transfer timed out (potential deadlock)");
        assertEquals(totalTransfers, successCount.get(), "All bidirectional transfers must succeed");
        assertEquals(0, failureCount.get(), "No deadlocks or exceptions allowed");

        // Verify balance conservation across all pairs
        for (int i = 0; i < pairCount; i++) {
            Account refreshedA = accountRepository.findById(aliceIds[i]).orElseThrow();
            Account refreshedB = accountRepository.findById(bobIds[i]).orElseThrow();
            BigDecimal pairTotal = refreshedA.getBalance().add(refreshedB.getBalance());
            assertEquals(initialPairBalance, pairTotal, "Total money in pair " + i + " must remain invariant");
            assertEquals(new BigDecimal("0.0000"), refreshedA.getLockedBalance());
            assertEquals(new BigDecimal("0.0000"), refreshedB.getLockedBalance());
        }
    }
}


