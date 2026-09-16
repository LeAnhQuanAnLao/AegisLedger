# Module Specification: Bank Simulation Engine (`simulation`)

## 1. Overview
The Bank Simulation Engine simulates the end-to-end operation of a commercial bank over a designated multi-day temporal period (e.g., 30 days / 1 month) with a large user population (e.g., 20,000 users). It generates realistic peer-to-peer transfers, fee deductions, daily spending limits, savings accounts, daily compound interest accrual, micro-lending credit scoring, loan disbursements, installment repayments, and nightly end-of-day (EOD) double-entry ledger audits.

To support high data volumes (over 500,000 ledger entries and 20,000 accounts), persistence is handled via high-throughput JDBC batch chunking (`JdbcTemplate.batchUpdate`), ensuring sub-minute execution times while strictly maintaining transactional consistency and the double-entry invariant ($\sum \text{Debit} = \sum \text{Credit}$).

---

## 2. Public API Contracts

### 2.1 REST Endpoints
- `POST /api/v1/simulation/run`
  - Request Body: `SimulationRequest`
    - `userCount` (int, default: 20000)
    - `days` (int, default: 30)
    - `dailyTransferCount` (int, default: 500)
    - `seedInitialDeposit` (boolean, default: true)
  - Response: `ApiResponse<SimulationProgressDto>` (HTTP 202 Accepted if async, or HTTP 200 OK)
- `GET /api/v1/simulation/status`
  - Response: `ApiResponse<SimulationProgressDto>`
    - `status` (`IDLE`, `RUNNING`, `COMPLETED`, `FAILED`)
    - `currentDay` (int)
    - `totalDays` (int)
    - `progressPercent` (double)
    - `totalTransactions` (long)
    - `totalLedgerEntries` (long)
    - `executionDurationMs` (long)
- `GET /api/v1/simulation/latest-report`
  - Response: `ApiResponse<SimulationResultDto>`
    - Detailed breakdown of 30 days, fees collected, interest exp/inc, and 30 EOD balance sheets.

### 2.2 Service Interface (`BankSimulationService`)
```java
public interface BankSimulationService {
    SimulationProgressDto startSimulation(SimulationConfig config);
    SimulationProgressDto getProgress();
    SimulationResultDto getLatestReport();
    SimulationResultDto runSynchronously(SimulationConfig config);
}
```

---

## 3. Data Flow & Simulation Phasing

### Phase 1: User Seeding
1. Generates $N$ user accounts (`ACC-SIM-00001` .. `ACC-SIM-N`) with varied balance tiers ($100 - $100,000).
2. Generates initial funding transactions:
   - Debit: `SYS-TREASURY`
   - Credit: `UserAccount`
   - Double-entry preserved ($\Delta = 0$).
3. Generates $N$ `daily_limit_configs` records (default $5,000 limit).
4. Persists all records via `SimulationBatchRepository` using 2,000-item chunks.

### Phase 2: 30-Day Operational Simulation
For each day $d \in [1, 30]$:
1. **Transfers (P2P)**:
   - Pairs of active users transfer funds.
   - Flat fee ($0.5000) deducted and credited to `SYS-FEE-REV`.
   - Daily limit usage recorded in `daily_limit_usages`.
   - 4 ledger entries: Debit sender, Credit receiver, Debit sender fee, Credit `SYS-FEE-REV`.
2. **Savings Activity**:
   - Users open term/demand savings accounts (funds locked to `SYS-SAVINGS-VAULT`).
   - Daily interest accrued across active savings accounts: $\text{daily} = \text{principal} \times \frac{\text{rate}}{365}$ (Debit `SYS-INTEREST-EXP`).
   - Settle matured deposits or process premature withdrawals at demand rate.
3. **Micro-Lending Activity**:
   - Users apply for micro-loans; credit scoring evaluated against ledger history.
   - Approved loans disbursed: Debit `SYS-TREASURY`, Credit User.
   - Equal principal + declining interest repayment schedule created.
   - Due installments collected via auto-debit: Principal $\rightarrow$ `SYS-TREASURY`, Interest $\rightarrow$ `SYS-INTEREST-INC`.
4. **EOD Engine Reconciliation**:
   - `EodReconciliationService.reconcileDailyAccounts(date)` executed at midnight of simulated day $d$.
   - Verifies $\sum \text{Debit} == \sum \text{Credit}$ and net account balance integrity.
   - Persists snapshot to `daily_balance_sheets`.

---

## 4. Dependencies
- `AccountRepository`, `LedgerRepository`, `DailyBalanceSheetRepository`
- `EodReconciliationService`
- `SavingsInterestCalculator`, `LoanAmortizationCalculator`
- `JdbcTemplate` for high-throughput batch writes
- `VirtualThreadConfig` for non-blocking concurrent simulation tasks

---

## 5. Invariants & Error Handling
- **Double-Entry Invariant**: $\sum \text{Debit} = \sum \text{Credit}$ at every stage.
- **Banker's Rounding**: All calculations rounded via `RoundingMode.HALF_EVEN` to 4 decimal places.
- **Simulation In-Progress**: If a simulation is already `RUNNING`, subsequent start requests reject with `BusinessException("Simulation already in progress")`.
