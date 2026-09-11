package com.aegisledger.account;

import com.aegisledger.account.domain.Account;
import com.aegisledger.account.dto.AccountDto;
import com.aegisledger.account.dto.CreateAccountRequest;
import com.aegisledger.account.repository.AccountRepository;
import com.aegisledger.account.service.AccountServiceImpl;
import com.aegisledger.core.domain.Currency;
import com.aegisledger.core.domain.Money;
import com.aegisledger.core.exception.AccountNotFoundException;
import com.aegisledger.core.exception.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for AccountService (Tier 1)")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private UUID accountId;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        testAccount = new Account(
            accountId,
            "ACC-1001",
            "Alice Smith",
            Money.of(new BigDecimal("1000.00"), Currency.USD)
        );
    }

    @Test
    @DisplayName("Should successfully create account with initial deposit")
    void testCreateAccountSuccess() {
        CreateAccountRequest request = new CreateAccountRequest(
            "ACC-1001",
            "Alice Smith",
            Currency.USD,
            new BigDecimal("1000.00")
        );
        when(accountRepository.existsByAccountNumber("ACC-1001")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        AccountDto created = accountService.createAccount(request);

        assertNotNull(created);
        assertEquals("ACC-1001", created.accountNumber());
        assertEquals(new BigDecimal("1000.0000"), created.balance());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("Should successfully hold funds and adjust available balance")
    void testHoldFundsSuccess() {
        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(testAccount));

        accountService.holdFunds(accountId, Money.of(200.00, Currency.USD));

        assertEquals(new BigDecimal("200.0000"), testAccount.getLockedBalance());
        assertEquals(new BigDecimal("800.0000"), testAccount.getAvailableBalance());
        assertEquals(new BigDecimal("1000.0000"), testAccount.getBalance());
        verify(accountRepository).save(testAccount);
    }

    @Test
    @DisplayName("Should throw InsufficientFundsException when holding more than available balance")
    void testHoldFundsInsufficient() {
        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(testAccount));

        Money overAmount = Money.of(1500.00, Currency.USD);
        assertThrows(InsufficientFundsException.class, () -> accountService.holdFunds(accountId, overAmount));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when account does not exist")
    void testAccountNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(accountRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.getAccount(nonExistentId));
    }
}
