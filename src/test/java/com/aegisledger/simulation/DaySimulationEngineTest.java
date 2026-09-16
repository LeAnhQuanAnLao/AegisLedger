package com.aegisledger.simulation;

import com.aegisledger.account.domain.Account;
import com.aegisledger.account.domain.AccountStatus;
import com.aegisledger.core.domain.Currency;
import com.aegisledger.core.domain.SystemAccounts;
import com.aegisledger.eod.dto.EodReportDto;
import com.aegisledger.eod.service.EodReconciliationService;
import com.aegisledger.simulation.domain.SimulationConfig;
import com.aegisledger.simulation.dto.DaySimulationReport;
import com.aegisledger.simulation.repository.SimulationBatchRepository;
import com.aegisledger.simulation.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DaySimulationEngineTest {

    @Mock
    private DayTransferSimulator transferSimulator;

    @Mock
    private DaySavingsSimulator savingsSimulator;

    @Mock
    private DayLoanSimulator loanSimulator;

    @Mock
    private SimulationBatchRepository batchRepository;

    @Mock
    private EodReconciliationService eodService;

    private DaySimulationEngineImpl dayEngine;

    @BeforeEach
    void setUp() {
        dayEngine = new DaySimulationEngineImpl(
            transferSimulator, savingsSimulator, loanSimulator, batchRepository, eodService
        );
    }

    @Test
    @DisplayName("Should simulate daily cycle, flush batches, and reconcile EOD successfully")
    void shouldSimulateDaySuccessfully() {
        LocalDate date = LocalDate.now();
        SimulationConfig config = SimulationConfig.testConfig(10, 1);

        Instant now = Instant.now();
        Account feeRev = new Account(SystemAccounts.FEE_REVENUE_ACCOUNT_ID, "SYS-FEE-REV", "Fee Rev",
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, Currency.USD, AccountStatus.ACTIVE);
        Account vault = new Account(SystemAccounts.SAVINGS_VAULT_ACCOUNT_ID, "SYS-VAULT", "Vault",
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, Currency.USD, AccountStatus.ACTIVE);
        Account treasury = new Account(SystemAccounts.TREASURY_ACCOUNT_ID, "SYS-TREASURY", "Treasury",
            new BigDecimal("1000000.0000"), BigDecimal.ZERO, new BigDecimal("1000000.0000"), Currency.USD, AccountStatus.ACTIVE);
        Account interestInc = new Account(SystemAccounts.INTEREST_INCOME_ACCOUNT_ID, "SYS-INT-INC", "Interest Inc",
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, Currency.USD, AccountStatus.ACTIVE);

        Map<UUID, Account> lookup = new HashMap<>();
        lookup.put(feeRev.getId(), feeRev);
        lookup.put(vault.getId(), vault);
        lookup.put(treasury.getId(), treasury);
        lookup.put(interestInc.getId(), interestInc);

        Account u1 = new Account(UUID.randomUUID(), "ACC-01", "User 1", new BigDecimal("500.0000"),
            BigDecimal.ZERO, new BigDecimal("500.0000"), Currency.USD, AccountStatus.ACTIVE);
        Account u2 = new Account(UUID.randomUUID(), "ACC-02", "User 2", new BigDecimal("500.0000"),
            BigDecimal.ZERO, new BigDecimal("500.0000"), Currency.USD, AccountStatus.ACTIVE);
        lookup.put(u1.getId(), u1);
        lookup.put(u2.getId(), u2);
        List<Account> accounts = List.of(u1, u2);

        when(transferSimulator.simulateTransfers(eq(date), anyInt(), anyList(), anyMap(), any()))
            .thenReturn(new DayTransferSimulator.TransferBatchResult(List.of(), List.of(), List.of(), BigDecimal.ZERO, BigDecimal.ZERO, Set.of()));

        when(savingsSimulator.simulateSavings(eq(date), anyInt(), anyList(), anyMap(), any(), anyList()))
            .thenReturn(new DaySavingsSimulator.SavingsBatchResult(List.of(), List.of(), List.of(), List.of(), BigDecimal.ZERO, BigDecimal.ZERO, Set.of()));

        when(loanSimulator.simulateLoans(eq(date), anyInt(), anyList(), anyMap(), any(), any(), anyList()))
            .thenReturn(new DayLoanSimulator.LoanBatchResult(List.of(), List.of(), List.of(), List.of(), List.of(), BigDecimal.ZERO, BigDecimal.ZERO, Set.of()));

        com.aegisledger.eod.domain.DailyAccountingBalanceSheet mockSheet = new com.aegisledger.eod.domain.DailyAccountingBalanceSheet(
            UUID.randomUUID(), date, 6, new BigDecimal("1001000.0000"), BigDecimal.ZERO, new BigDecimal("1001000.0000"),
            new BigDecimal("10000.0000"), new BigDecimal("10000.0000"), true, 0, null,
            com.aegisledger.eod.domain.ReconciliationStatus.BALANCED, 15L
        );
        when(eodService.runReconciliation(date)).thenReturn(mockSheet);

        DaySimulationReport report = dayEngine.simulateDay(1, date, config, accounts, lookup);

        assertThat(report).isNotNull();
        assertThat(report.dayIndex()).isEqualTo(1);
        assertThat(report.eodBalanced()).isTrue();
        verify(eodService, times(1)).runReconciliation(date);
    }
}
