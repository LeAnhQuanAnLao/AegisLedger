package com.aegisledger.simulation;

import com.aegisledger.account.domain.Account;
import com.aegisledger.account.domain.AccountStatus;
import com.aegisledger.account.repository.AccountRepository;
import com.aegisledger.core.domain.Currency;
import com.aegisledger.simulation.domain.SimulationConfig;
import com.aegisledger.simulation.domain.SimulationStatus;
import com.aegisledger.simulation.dto.DaySimulationReport;
import com.aegisledger.simulation.dto.SimulationProgressDto;
import com.aegisledger.simulation.dto.SimulationResultDto;
import com.aegisledger.simulation.service.BankSimulationServiceImpl;
import com.aegisledger.simulation.service.DaySimulationEngine;
import com.aegisledger.simulation.service.UserSeedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankSimulationServiceTest {

    @Mock
    private UserSeedService userSeedService;

    @Mock
    private DaySimulationEngine dayEngine;

    @Mock
    private AccountRepository accountRepository;

    private BankSimulationServiceImpl simulationService;

    @BeforeEach
    void setUp() {
        simulationService = new BankSimulationServiceImpl(userSeedService, dayEngine, accountRepository);
    }

    @Test
    @DisplayName("Should run simulation synchronously and return final result")
    void shouldRunSimulationSynchronously() {
        SimulationConfig config = SimulationConfig.testConfig(10, 2);
        Account acc = new Account(UUID.randomUUID(), "ACC-01", "User 1", new BigDecimal("500.0000"),
            BigDecimal.ZERO, new BigDecimal("500.0000"), Currency.USD, AccountStatus.ACTIVE);

        when(userSeedService.seedUsers(eq(10), anyBoolean())).thenReturn(List.of(acc));
        when(accountRepository.findAll()).thenReturn(List.of(acc));

        DaySimulationReport day1 = new DaySimulationReport(1, LocalDate.now().minusDays(1), 5,
            new BigDecimal("100.0000"), new BigDecimal("2.5000"), 1, new BigDecimal("100.0000"),
            new BigDecimal("0.0500"), 1, new BigDecimal("500.0000"), BigDecimal.ZERO, true, 10L);

        DaySimulationReport day2 = new DaySimulationReport(2, LocalDate.now(), 5,
            new BigDecimal("100.0000"), new BigDecimal("2.5000"), 1, new BigDecimal("100.0000"),
            new BigDecimal("0.0500"), 0, BigDecimal.ZERO, new BigDecimal("50.0000"), true, 10L);

        when(dayEngine.simulateDay(eq(1), any(), eq(config), anyList(), anyMap())).thenReturn(day1);
        when(dayEngine.simulateDay(eq(2), any(), eq(config), anyList(), anyMap())).thenReturn(day2);

        SimulationResultDto result = simulationService.runSynchronously(config);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(SimulationStatus.COMPLETED);
        assertThat(result.daysSimulated()).isEqualTo(2);
        assertThat(result.totalTransactions()).isEqualTo(10);
        assertThat(result.allEodReconciled()).isTrue();
    }

    @Test
    @DisplayName("Should return idle status initially")
    void shouldReturnIdleStatusInitially() {
        SimulationProgressDto progress = simulationService.getProgress();
        assertThat(progress.status()).isEqualTo(SimulationStatus.IDLE);
        assertThat(progress.currentDay()).isEqualTo(0);
    }
}
