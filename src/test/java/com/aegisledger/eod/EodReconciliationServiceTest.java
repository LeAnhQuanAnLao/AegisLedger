package com.aegisledger.eod;

import com.aegisledger.account.domain.Account;
import com.aegisledger.account.repository.AccountRepository;
import com.aegisledger.core.domain.Currency;
import com.aegisledger.core.domain.Money;
import com.aegisledger.eod.domain.DailyAccountingBalanceSheet;
import com.aegisledger.eod.domain.ReconciliationStatus;
import com.aegisledger.eod.repository.DailyBalanceSheetRepository;
import com.aegisledger.eod.service.EodReconciliationService;
import com.aegisledger.eod.service.EodReconciliationServiceImpl;
import com.aegisledger.ledger.domain.EntryType;
import com.aegisledger.ledger.repository.LedgerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EodReconciliationServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private DailyBalanceSheetRepository balanceSheetRepository;

    private EodReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        reconciliationService = new EodReconciliationServiceImpl(
            accountRepository, ledgerRepository, balanceSheetRepository
        );
    }

    @Test
    @DisplayName("Should successfully reconcile when ledger debits equal credits and accounts are sound")
    void shouldReconcileSuccessfullyWhenBalanced() {
        // Arrange
        LocalDate today = LocalDate.now();
        Account acc1 = new Account(UUID.randomUUID(), "ACC-01", "Alice", Money.of(1000.00, Currency.USD));
        Account acc2 = new Account(UUID.randomUUID(), "ACC-02", "Bob", Money.of(2000.00, Currency.USD));

        when(ledgerRepository.sumTotalByEntryType(EntryType.DEBIT)).thenReturn(new BigDecimal("5000.0000"));
        when(ledgerRepository.sumTotalByEntryType(EntryType.CREDIT)).thenReturn(new BigDecimal("5000.0000"));
        when(accountRepository.findAll()).thenReturn(List.of(acc1, acc2));
        when(balanceSheetRepository.findByReconciliationDate(today)).thenReturn(Optional.empty());
        when(balanceSheetRepository.save(any(DailyAccountingBalanceSheet.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        DailyAccountingBalanceSheet result = reconciliationService.runReconciliation(today);

        // Assert
        assertNotNull(result);
        assertEquals(ReconciliationStatus.BALANCED, result.getStatus());
        assertTrue(result.isLedgerBalanced());
        assertEquals(2, result.getTotalAccountsChecked());
        assertEquals(new BigDecimal("3000.0000"), result.getTotalAccountBalance());
        assertEquals(0, result.getDiscrepancyCount());

        ArgumentCaptor<DailyAccountingBalanceSheet> captor = ArgumentCaptor.forClass(DailyAccountingBalanceSheet.class);
        verify(balanceSheetRepository).save(captor.capture());
        assertEquals(ReconciliationStatus.BALANCED, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("Should flag DISCREPANCY_DETECTED when total debits do not equal total credits")
    void shouldDetectDiscrepancyWhenLedgerImbalanced() {
        // Arrange
        LocalDate today = LocalDate.now();
        Account acc = new Account(UUID.randomUUID(), "ACC-01", "Alice", Money.of(1000.00, Currency.USD));

        when(ledgerRepository.sumTotalByEntryType(EntryType.DEBIT)).thenReturn(new BigDecimal("5000.0000"));
        when(ledgerRepository.sumTotalByEntryType(EntryType.CREDIT)).thenReturn(new BigDecimal("4900.0000")); // Mismatch!
        when(accountRepository.findAll()).thenReturn(List.of(acc));
        when(balanceSheetRepository.findByReconciliationDate(today)).thenReturn(Optional.empty());
        when(balanceSheetRepository.save(any(DailyAccountingBalanceSheet.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        DailyAccountingBalanceSheet result = reconciliationService.runReconciliation(today);

        // Assert
        assertNotNull(result);
        assertEquals(ReconciliationStatus.DISCREPANCY_DETECTED, result.getStatus());
        assertFalse(result.isLedgerBalanced());
    }
}
