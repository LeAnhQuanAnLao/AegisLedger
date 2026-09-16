package com.aegisledger.integration;

import com.aegisledger.account.repository.AccountRepository;
import com.aegisledger.eod.domain.DailyAccountingBalanceSheet;
import com.aegisledger.eod.domain.ReconciliationStatus;
import com.aegisledger.eod.repository.DailyBalanceSheetRepository;
import com.aegisledger.ledger.domain.EntryType;
import com.aegisledger.ledger.repository.LedgerRepository;
import com.aegisledger.payment.repository.TransactionRepository;
import com.aegisledger.simulation.domain.SimulationConfig;
import com.aegisledger.simulation.domain.SimulationStatus;
import com.aegisledger.simulation.dto.SimulationResultDto;
import com.aegisledger.simulation.service.BankSimulationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class BankSimulationIntegrationTest {

    @Autowired
    private BankSimulationService simulationService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LedgerRepository ledgerRepository;

    @Autowired
    private DailyBalanceSheetRepository balanceSheetRepository;

    @Test
    @DisplayName("Should execute multi-day bank simulation, persist all records to DB, and maintain double-entry equilibrium")
    void shouldExecuteSimulationAndPersistToDb() {
        int users = 200;
        int days = 3;
        SimulationConfig config = new SimulationConfig(users, days, 50, 10, 5, true, 500);

        SimulationResultDto result = simulationService.runSynchronously(config);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(SimulationStatus.COMPLETED);
        assertThat(result.daysSimulated()).isEqualTo(days);
        assertThat(result.allEodReconciled()).isTrue();

        // 1. Verify accounts persisted in DB
        long accountCount = accountRepository.count();
        assertThat(accountCount).isGreaterThanOrEqualTo(users + 5); // 200 users + 5 system accounts

        // 2. Verify transactions and ledger entries persisted in DB
        long txCount = transactionRepository.count();
        long ledgerCount = ledgerRepository.count();
        assertThat(txCount).isGreaterThan(0);
        assertThat(ledgerCount).isGreaterThan(txCount);

        // 3. Verify double-entry ledger equilibrium in Database (Sum Debit == Sum Credit)
        BigDecimal totalDebits = ledgerRepository.sumTotalByEntryType(EntryType.DEBIT);
        BigDecimal totalCredits = ledgerRepository.sumTotalByEntryType(EntryType.CREDIT);

        assertThat(totalDebits).isGreaterThan(BigDecimal.ZERO);
        assertThat(totalDebits).isEqualByComparingTo(totalCredits);

        // 4. Verify EOD balance sheets generated in DB
        List<DailyAccountingBalanceSheet> sheets = balanceSheetRepository.findAll();
        assertThat(sheets).hasSize(days);
        for (DailyAccountingBalanceSheet sheet : sheets) {
            assertThat(sheet.getStatus()).isEqualTo(ReconciliationStatus.BALANCED);
            assertThat(sheet.isLedgerBalanced()).isTrue();
            assertThat(sheet.getDiscrepancyCount()).isZero();
        }
    }

    @Test
    @DisplayName("Should simulate 1 month of banking operations for 20000 users and persist to DB")
    void shouldSimulate20000UsersOneMonth() {
        int users = 20000;
        int days = 30;
        SimulationConfig config = new SimulationConfig(users, days, 200, 20, 10, true, 2000);

        SimulationResultDto result = simulationService.runSynchronously(config);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(SimulationStatus.COMPLETED);
        assertThat(result.userCount()).isEqualTo(20000);
        assertThat(result.daysSimulated()).isEqualTo(30);
        assertThat(result.allEodReconciled()).isTrue();

        // 1. Verify 20000 users persisted in DB
        assertThat(accountRepository.count()).isGreaterThanOrEqualTo(20005);

        // 2. Verify all 30 days of EOD balance sheets generated in DB
        assertThat(balanceSheetRepository.findAll()).hasSize(days);

        // 3. Verify total debits == total credits in Database
        BigDecimal totalDebits = ledgerRepository.sumTotalByEntryType(EntryType.DEBIT);
        BigDecimal totalCredits = ledgerRepository.sumTotalByEntryType(EntryType.CREDIT);
        assertThat(totalDebits).isGreaterThan(BigDecimal.ZERO);
        assertThat(totalDebits).isEqualByComparingTo(totalCredits);
    }
}
