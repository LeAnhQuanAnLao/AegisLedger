package com.aegisledger.ledger;

import com.aegisledger.account.domain.Account;
import com.aegisledger.account.repository.AccountRepository;
import com.aegisledger.core.domain.Currency;
import com.aegisledger.core.domain.Money;
import com.aegisledger.core.exception.InsufficientFundsException;
import com.aegisledger.ledger.domain.EntryType;
import com.aegisledger.ledger.domain.LedgerEntry;
import com.aegisledger.ledger.dto.LedgerEntryDto;
import com.aegisledger.ledger.repository.LedgerRepository;
import com.aegisledger.ledger.service.DoubleEntryLedgerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for DoubleEntryLedgerService (Tier 2)")
class DoubleEntryLedgerServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LedgerRepository ledgerRepository;

    @InjectMocks
    private DoubleEntryLedgerServiceImpl ledgerService;

    private UUID sourceId;
    private UUID destId;
    private Account sourceAccount;
    private Account destAccount;

    @BeforeEach
    void setUp() {
        sourceId = UUID.randomUUID();
        destId = UUID.randomUUID();
        sourceAccount = new Account(sourceId, "ACC-SRC", "Sender", Money.of(1000.00, Currency.USD));
        destAccount = new Account(destId, "ACC-DST", "Receiver", Money.of(500.00, Currency.USD));
    }

    @Test
    @DisplayName("Should create balanced Debit and Credit ledger entries preserving total balance")
    void testRecordTransferPreservesDoubleEntryBalance() {
        UUID txId = UUID.randomUUID();
        Money transferAmount = Money.of(200.00, Currency.USD);

        when(accountRepository.findByIdForUpdate(sourceId)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIdForUpdate(destId)).thenReturn(Optional.of(destAccount));

        when(ledgerRepository.save(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<LedgerEntryDto> entries = ledgerService.recordTransfer(
            txId, sourceId, destId, transferAmount, "Payment transfer", false
        );

        assertNotNull(entries);
        assertEquals(2, entries.size());

        LedgerEntryDto debit = entries.stream().filter(e -> e.entryType() == EntryType.DEBIT).findFirst().orElseThrow();
        LedgerEntryDto credit = entries.stream().filter(e -> e.entryType() == EntryType.CREDIT).findFirst().orElseThrow();

        // Strict double-entry invariant: Sum(Debit) == Sum(Credit) == Amount
        assertEquals(transferAmount.getAmount(), debit.amount());
        assertEquals(transferAmount.getAmount(), credit.amount());
        assertEquals(new BigDecimal("800.0000"), sourceAccount.getBalance());
        assertEquals(new BigDecimal("700.0000"), destAccount.getBalance());

        // Overall money conserved: (1000 + 500) == (800 + 700) = 1500
        assertEquals(
            new BigDecimal("1500.0000"),
            sourceAccount.getBalance().add(destAccount.getBalance())
        );
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if source and destination are identical")
    void testSameAccountThrowsException() {
        UUID txId = UUID.randomUUID();
        Money amount = Money.of(50.00, Currency.USD);

        assertThrows(IllegalArgumentException.class, () ->
            ledgerService.recordTransfer(txId, sourceId, sourceId, amount, "Invalid", false)
        );
    }
}
