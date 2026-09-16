package com.aegisledger.lending;

import com.aegisledger.account.domain.Account;
import com.aegisledger.account.domain.AccountStatus;
import com.aegisledger.account.repository.AccountRepository;
import com.aegisledger.core.domain.Currency;
import com.aegisledger.core.domain.Money;
import com.aegisledger.lending.dto.CreditScoreResult;
import com.aegisledger.lending.service.CreditScoringService;
import com.aegisledger.lending.service.CreditScoringServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditScoringServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private CreditScoringService creditScoringService;
    private final UUID accountId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        creditScoringService = new CreditScoringServiceImpl(accountRepository);
    }

    @Test
    @DisplayName("Should approve loan when account has good balance and requested amount within limit")
    void shouldApproveLoanForGoodBalance() {
        // Arrange: Account has $2,000 balance -> score: 40 base + 20 (>=1000) + 30 (requested <= max) = 90
        Account account = new Account(accountId, "ACC-01", "Alice", Money.of(2000.00, Currency.USD));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // Act: Request $1,500 loan (3x of 2000 is 6000 max)
        CreditScoreResult result = creditScoringService.evaluate(accountId, new BigDecimal("1500.0000"), 6);

        // Assert
        assertTrue(result.approved());
        assertTrue(result.score() >= 60);
        assertEquals(new BigDecimal("6000.0000"), result.maxAllowedLoan());
        assertEquals(new BigDecimal("0.1000"), result.interestRate());
    }

    @Test
    @DisplayName("Should reject loan when requested amount exceeds max allowed loan")
    void shouldRejectWhenRequestedExceedsMax() {
        // Arrange: Account has $100 balance -> max allowed = $500
        Account account = new Account(accountId, "ACC-02", "Bob", Money.of(100.00, Currency.USD));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // Act: Request $2,000 loan
        CreditScoreResult result = creditScoringService.evaluate(accountId, new BigDecimal("2000.0000"), 6);

        // Assert
        assertFalse(result.approved());
    }

    @Test
    @DisplayName("Should reject loan when account is not active")
    void shouldRejectWhenAccountNotActive() {
        // Arrange
        Account account = new Account(accountId, "ACC-03", "Charlie", Money.of(10000.00, Currency.USD));
        account.setStatus(AccountStatus.FROZEN);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // Act
        CreditScoreResult result = creditScoringService.evaluate(accountId, new BigDecimal("1000.0000"), 6);

        // Assert
        assertFalse(result.approved());
        assertEquals(0, result.score());
    }
}
