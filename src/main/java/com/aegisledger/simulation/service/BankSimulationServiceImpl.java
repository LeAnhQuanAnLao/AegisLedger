package com.aegisledger.simulation.service;

import com.aegisledger.account.domain.Account;
import com.aegisledger.account.repository.AccountRepository;
import com.aegisledger.core.domain.SystemAccounts;
import com.aegisledger.simulation.domain.SimulationConfig;
import com.aegisledger.simulation.domain.SimulationStatus;
import com.aegisledger.simulation.dto.DaySimulationReport;
import com.aegisledger.simulation.dto.SimulationProgressDto;
import com.aegisledger.simulation.dto.SimulationResultDto;
import com.aegisledger.simulation.exception.SimulationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Coordinates end-to-end multi-day banking simulation with progress monitoring.
 */
@Service
public class BankSimulationServiceImpl implements BankSimulationService {

    private static final Logger log = LoggerFactory.getLogger(BankSimulationServiceImpl.class);

    private final UserSeedService userSeedService;
    private final DaySimulationEngine dayEngine;
    private final AccountRepository accountRepository;

    private volatile SimulationStatus status = SimulationStatus.IDLE;
    private volatile int currentDay = 0;
    private volatile int totalDays = 0;
    private volatile long totalTransactions = 0;
    private volatile long totalLedgerEntries = 0;
    private volatile long totalSavingsCount = 0;
    private volatile long totalLoansCount = 0;
    private volatile long startTimeMs = 0;
    private volatile long executionDurationMs = 0;
    private volatile String message = "Simulation ready";
    private volatile SimulationResultDto latestReport = null;

    public BankSimulationServiceImpl(
        UserSeedService userSeedService,
        DaySimulationEngine dayEngine,
        AccountRepository accountRepository
    ) {
        this.userSeedService = userSeedService;
        this.dayEngine = dayEngine;
        this.accountRepository = accountRepository;
    }

    @Override
    public synchronized SimulationProgressDto startSimulation(SimulationConfig config) {
        if (status == SimulationStatus.RUNNING) {
            throw new SimulationException("A simulation is already in progress");
        }
        initRunState(config.days());
        CompletableFuture.runAsync(() -> executeRun(config));
        return getProgress();
    }

    @Override
    public synchronized SimulationResultDto runSynchronously(SimulationConfig config) {
        if (status == SimulationStatus.RUNNING) {
            throw new SimulationException("A simulation is already in progress");
        }
        initRunState(config.days());
        return executeRun(config);
    }

    @Override
    public SimulationProgressDto getProgress() {
        double pct = totalDays > 0 ? (currentDay * 100.0) / totalDays : 0.0;
        long dur = status == SimulationStatus.RUNNING ? System.currentTimeMillis() - startTimeMs : executionDurationMs;
        return new SimulationProgressDto(status, currentDay, totalDays, pct, totalTransactions,
            totalLedgerEntries, totalSavingsCount, totalLoansCount, dur, message);
    }

    @Override
    public SimulationResultDto getLatestReport() {
        return latestReport;
    }

    private void initRunState(int days) {
        this.status = SimulationStatus.RUNNING;
        this.currentDay = 0;
        this.totalDays = days;
        this.totalTransactions = 0;
        this.totalLedgerEntries = 0;
        this.totalSavingsCount = 0;
        this.totalLoansCount = 0;
        this.startTimeMs = System.currentTimeMillis();
        this.executionDurationMs = 0;
        this.message = "Initializing simulation...";
    }

    private SimulationResultDto executeRun(SimulationConfig config) {
        try {
            log.info("Starting simulation run for {} users across {} days...", config.userCount(), config.days());
            message = "Seeding " + config.userCount() + " accounts...";
            List<Account> accounts = userSeedService.seedUsers(config.userCount(), config.seedInitialDeposit());

            Map<UUID, Account> lookup = loadAccountLookup(accounts);
            LocalDate startDate = LocalDate.now().minusDays(config.days());
            List<DaySimulationReport> dailyReports = new ArrayList<>(config.days());

            for (int d = 1; d <= config.days(); d++) {
                currentDay = d;
                LocalDate simDate = startDate.plusDays(d - 1);
                message = String.format("Simulating Day %d of %d (%s)...", d, config.days(), simDate);

                DaySimulationReport report = dayEngine.simulateDay(d, simDate, config, accounts, lookup);
                dailyReports.add(report);
                accumulateTotals(report);
            }

            this.executionDurationMs = System.currentTimeMillis() - startTimeMs;
            this.status = SimulationStatus.COMPLETED;
            this.message = "Simulation successfully completed in " + executionDurationMs + "ms";
            this.latestReport = buildFinalReport(config, dailyReports);
            log.info("Simulation completed: {}", message);
            return latestReport;
        } catch (Exception e) {
            log.error("Simulation run failed: {}", e.getMessage(), e);
            this.status = SimulationStatus.FAILED;
            this.message = "Simulation failed: " + e.getMessage();
            throw new SimulationException("Simulation failed: " + e.getMessage());
        }
    }

    private Map<UUID, Account> loadAccountLookup(List<Account> seeded) {
        Map<UUID, Account> map = new HashMap<>(seeded.size() + 10);
        for (Account a : seeded) map.put(a.getId(), a);
        accountRepository.findAll().forEach(a -> map.put(a.getId(), a));
        return map;
    }

    private void accumulateTotals(DaySimulationReport r) {
        totalTransactions += r.transferCount();
        totalLedgerEntries += r.transferCount() * 4;
        totalSavingsCount += r.savingsOpenedCount();
        totalLoansCount += r.loansDisbursedCount();
    }

    private SimulationResultDto buildFinalReport(SimulationConfig config, List<DaySimulationReport> daily) {
        BigDecimal totalVol = daily.stream().map(DaySimulationReport::totalTransferVolume).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFees = daily.stream().map(DaySimulationReport::totalFeesCollected).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSavingsPrincipal = daily.stream().map(DaySimulationReport::totalSavingsPrincipal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalInterestExp = daily.stream().map(DaySimulationReport::dailyInterestAccrued).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLoanVol = daily.stream().map(DaySimulationReport::totalLoansDisbursed).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalInterestInc = daily.stream().map(DaySimulationReport::loanRepaymentsCollected).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean allBalanced = daily.stream().allMatch(DaySimulationReport::eodBalanced);

        return new SimulationResultDto(SimulationStatus.COMPLETED, config.userCount(), config.days(),
            totalTransactions, totalLedgerEntries, totalVol, totalFees, totalSavingsCount, totalSavingsPrincipal,
            totalInterestExp, totalLoansCount, totalLoanVol, totalInterestInc, allBalanced, executionDurationMs, daily);
    }
}
