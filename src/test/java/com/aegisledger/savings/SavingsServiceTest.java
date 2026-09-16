package com.aegisledger.savings;

import com.aegisledger.account.domain.Account;
import com.aegisledger.account.repository.AccountRepository;
import com.aegisledger.core.domain.Currency;
import com.aegisledger.core.domain.Money;
import com.aegisledger.core.domain.SystemAccounts;
import com.aegisledger.ledger.service.DoubleEntryLedgerService;
import com.aegisledger.savings.domain.RolloverOption;
import com.aegisledger.savings.domain.SavingsAccount;
import com.aegisledger.savings.domain.SavingsStatus;
import com.aegisledger.savings.dto.OpenSavingsRequest;
import com.aegisledger.savings.dto.PrematureWithdrawalResult;
import com.aegisledger.savings.dto.SavingsDto;
import com.aegisledger.savings.repository.SavingsAccountRepository;
import com.aegisledger.savings.service.SavingsInterestCalculator;
import com.aegisledger.savings.service.SavingsService;
import com.aegisledger.savings.service.SavingsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavingsServiceTest {

    @Mock
    private SavingsAccountRepository savingsRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private DoubleEntryLedgerService ledgerService;

    @Spy
    private SavingsInterestCalculator calculator = new SavingsInterestCalculator();

    private SavingsService savingsService;

    private final UUID accountId = UUID.randomUUID();
    private Account customerAccount;

    @BeforeEach
    void setUp() {
        savingsService = new SavingsServiceImpl(savingsRepository, accountRepository, ledgerService, calculator);
        customerAccount = new Account(
            accountId, "ACC-123456", "John Doe", Money.of(20000.00, Currency.USD)
        );
    }

    @Test
    @DisplayName("Should successfully open savings account and record double-entry transfer to vault")
    void shouldOpenSavingsAccountSuccessfully() {
        // Arrange
        OpenSavingsRequest request = new OpenSavingsRequest(
            accountId, new BigDecimal("5000.0000"), 6, RolloverOption.AUTO_SETTLE
        );
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(customerAccount));
        when(savingsRepository.save(any(SavingsAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        SavingsDto dto = savingsService.openSavings(request);

        // Assert
        assertNotNull(dto);
        assertEquals(accountId, dto.accountId());
        assertEquals(new BigDecimal("5000.0000"), dto.principalAmount());
        assertEquals(6, dto.termMonths());
        assertEquals(SavingsStatus.ACTIVE, dto.status());

        // Verify ledger transfer from customer to Savings Vault
        verify(ledgerService).recordTransfer(
            any(UUID.class),
            eq(accountId),
            eq(SystemAccounts.SAVINGS_VAULT_ACCOUNT_ID),
            eq(Money.of(5000.00, Currency.USD)),
            anyString(),
            eq(false)
        );
    }

    @Test
    @DisplayName("Should accrue daily interest for active savings accounts")
    void shouldAccrueDailyInterest() {
        // Arrange
        LocalDate today = LocalDate.now();
        SavingsAccount savings = new SavingsAccount(
            UUID.randomUUID(), accountId, "SAV-101", new BigDecimal("10000.0000"),
            new BigDecimal("0.0680"), 12, RolloverOption.AUTO_SETTLE, today.minusDays(10)
        );
        when(savingsRepository.findByStatus(SavingsStatus.ACTIVE)).thenReturn(List.of(savings));

        // Act
        int accruedCount = savingsService.accrueDailyInterest(today);

        // Assert
        assertEquals(1, accruedCount);
        assertEquals(new BigDecimal("1.8630"), savings.getAccruedInterest());
        assertEquals(today, savings.getLastAccrualDate());
        verify(savingsRepository).save(savings);
    }

    @Test
    @DisplayName("Should process premature withdrawal, forfeit term interest and pay demand interest")
    void shouldProcessPrematureWithdrawal() {
        // Arrange
        UUID savingsId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().minusDays(30);
        SavingsAccount savings = new SavingsAccount(
            savingsId, accountId, "SAV-PREM", new BigDecimal("10000.0000"),
            new BigDecimal("0.0680"), 12, RolloverOption.AUTO_SETTLE, startDate
        );
        savings.setAccruedInterest(new BigDecimal("55.8900")); // Accrued at 6.8%

        when(savingsRepository.findById(savingsId)).thenReturn(Optional.of(savings));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(customerAccount));

        // Act
        PrematureWithdrawalResult result = savingsService.withdrawPrematurely(savingsId);

        // Assert
        assertNotNull(result);
        assertEquals(SavingsStatus.PREMATURE_WITHDRAWN, savings.getStatus());
        assertTrue(result.actualNonTermInterestPaid().compareTo(savings.getAccruedInterest()) < 0);
        assertTrue(result.forfeitedTermInterest().compareTo(BigDecimal.ZERO) > 0);

        // Verify principal returned to customer
        verify(ledgerService).recordTransfer(
            any(UUID.class), eq(SystemAccounts.SAVINGS_VAULT_ACCOUNT_ID), eq(accountId),
            eq(Money.of(10000.00, Currency.USD)), anyString(), eq(false)
        );
    }

    @Test
    @DisplayName("Should auto-settle principal and interest on maturity")
    void shouldAutoSettleOnMaturity() {
        // Arrange
        LocalDate today = LocalDate.now();
        SavingsAccount savings = new SavingsAccount(
            UUID.randomUUID(), accountId, "SAV-MATURE", new BigDecimal("5000.0000"),
            new BigDecimal("0.0550"), 6, RolloverOption.AUTO_SETTLE, today.minusMonths(6)
        );
        savings.setMaturityDate(today);
        savings.setAccruedInterest(new BigDecimal("137.5000"));

        when(savingsRepository.findByStatusAndMaturityDateLessThanEqual(eq(SavingsStatus.ACTIVE), eq(today)))
            .thenReturn(List.of(savings));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(customerAccount));

        // Act
        int maturedCount = savingsService.processMaturities(today);

        // Assert
        assertEquals(1, maturedCount);
        assertEquals(SavingsStatus.SETTLED, savings.getStatus());

        // Verify principal returned and interest paid
        verify(ledgerService).recordTransfer(
            any(UUID.class), eq(SystemAccounts.SAVINGS_VAULT_ACCOUNT_ID), eq(accountId),
            eq(Money.of(5000.00, Currency.USD)), anyString(), eq(false)
        );
        verify(ledgerService).recordTransfer(
            any(UUID.class), eq(SystemAccounts.INTEREST_EXPENSE_ACCOUNT_ID), eq(accountId),
            eq(Money.of(137.50, Currency.USD)), anyString(), eq(false)
        );
    }
}
