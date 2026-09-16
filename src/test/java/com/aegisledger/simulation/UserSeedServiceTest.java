package com.aegisledger.simulation;

import com.aegisledger.account.domain.Account;
import com.aegisledger.account.domain.AccountStatus;
import com.aegisledger.account.repository.AccountRepository;
import com.aegisledger.core.domain.Currency;
import com.aegisledger.core.domain.SystemAccounts;
import com.aegisledger.simulation.repository.SimulationBatchRepository;
import com.aegisledger.simulation.service.UserSeedServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSeedServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private SimulationBatchRepository batchRepository;

    private UserSeedServiceImpl userSeedService;

    @BeforeEach
    void setUp() {
        userSeedService = new UserSeedServiceImpl(accountRepository, batchRepository);
    }

    @Test
    @DisplayName("Should seed requested count of users and persist via batch repository")
    void shouldSeedUsersSuccessfully() {
        Account treasury = new Account(SystemAccounts.TREASURY_ACCOUNT_ID, "SYS-TREASURY", "Treasury",
            new BigDecimal("10000000.0000"), BigDecimal.ZERO, new BigDecimal("10000000.0000"),
            Currency.USD, AccountStatus.ACTIVE);

        when(accountRepository.findById(SystemAccounts.TREASURY_ACCOUNT_ID)).thenReturn(Optional.of(treasury));

        List<Account> seeded = userSeedService.seedUsers(10, true);

        assertThat(seeded).hasSize(10);
        verify(batchRepository, times(1)).batchInsertAccounts(anyList());
        verify(batchRepository, times(1)).batchInsertDailyLimitConfigs(anyList());
        verify(batchRepository, times(1)).batchInsertTransactions(anyList());
        verify(batchRepository, times(1)).batchInsertLedgerEntries(anyList());
        verify(batchRepository, times(1)).batchUpdateAccountBalances(anyList());
    }

    @Test
    @DisplayName("Should create users without initial funding if deposit is disabled")
    void shouldSeedUsersWithoutInitialDeposit() {
        Account treasury = new Account(SystemAccounts.TREASURY_ACCOUNT_ID, "SYS-TREASURY", "Treasury",
            new BigDecimal("10000000.0000"), BigDecimal.ZERO, new BigDecimal("10000000.0000"),
            Currency.USD, AccountStatus.ACTIVE);

        when(accountRepository.findById(SystemAccounts.TREASURY_ACCOUNT_ID)).thenReturn(Optional.of(treasury));

        List<Account> seeded = userSeedService.seedUsers(5, false);

        assertThat(seeded).hasSize(5);
        verify(batchRepository, times(1)).batchInsertAccounts(anyList());
        verify(batchRepository, times(1)).batchInsertDailyLimitConfigs(anyList());
        verify(batchRepository, never()).batchInsertTransactions(anyList());
    }
}
