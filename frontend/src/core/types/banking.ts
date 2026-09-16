/**
 * Core Domain Types for AegisLedger Core Banking & Payment Orchestration Engine.
 * Strict 1:1 alignment with Spring Boot 3 Backend DTOs & Domain Entities.
 */

export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
  timestamp: string;
}

export type Currency = 'USD' | 'EUR' | 'VND' | 'SGD';

export type AccountStatus = 'ACTIVE' | 'LOCKED' | 'SUSPENDED' | 'CLOSED';

export interface AccountDto {
  id: string;
  accountNumber: string;
  holderName: string;
  balance: number;
  lockedBalance: number;
  availableBalance: number;
  currency: Currency;
  status: AccountStatus;
  createdAt: string;
}

export interface CreateAccountRequest {
  accountNumber: string;
  holderName: string;
  currency: Currency;
  initialDeposit: number;
}

export type TransactionStatus =
  | 'PENDING'
  | 'EXECUTING'
  | 'COMPLETED'
  | 'COMPENSATED'
  | 'FAILED';

export type SagaStep =
  | 'STARTED'
  | 'FUNDS_HELD'
  | 'FRAUD_EVALUATED'
  | 'SWITCH_PROCESSED'
  | 'COMMITTED'
  | 'COMPENSATED';

export interface TransferRequest {
  sourceAccountId: string;
  destinationAccountId: string;
  amount: number;
  currency: Currency;
  idempotencyKey: string;
  description?: string;
}

export interface TransferResponse {
  transactionId: string;
  idempotencyKey: string;
  status: TransactionStatus;
  currentStep: SagaStep;
  message: string;
  timestamp: string;
}

export interface Transaction {
  id: string;
  idempotencyKey: string;
  sourceAccountId: string;
  destinationAccountId: string;
  amount: number;
  currency: Currency;
  status: TransactionStatus;
  sagaStep: SagaStep;
  failureReason?: string | null;
  createdAt: string;
}

export type EntryType = 'DEBIT' | 'CREDIT';

export interface LedgerEntryDto {
  id: string;
  transactionId: string;
  accountId: string;
  entryType: EntryType;
  amount: number;
  balanceAfter: number;
  description: string;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface FraudRuleTelemetry {
  slidingWindowCount: number;
  slidingWindowMax: number;
  windowSeconds: number;
  velocityStatus: 'NORMAL' | 'HIGH' | 'CRITICAL';
  largeAmountThreshold: number;
  riskScore: number;
  isHighRiskHours: boolean;
}

export interface SystemMetrics {
  status: 'UP' | 'DOWN' | 'DEGRADED';
  virtualThreadsActive: boolean;
  dbPoolActive: number;
  dbPoolMax: number;
  outboxPendingCount: number;
  uptimeSeconds: number;
  avgLatencyMs: number;
}

export interface DailyLimitStatusDto {
  accountId: string;
  date: string;
  configuredLimit: number;
  totalSpentToday: number;
  remainingLimit: number;
}

export interface FeeCalculationResult {
  transferAmount: { amount: number; currency: Currency };
  feeAmount: { amount: number; currency: Currency };
  totalDebitAmount: { amount: number; currency: Currency };
}

export interface SavingsDto {
  id: string;
  accountId: string;
  savingsNumber: string;
  principalAmount: number;
  interestRate: number;
  termMonths: number;
  rolloverOption: 'PRINCIPAL_AND_INTEREST' | 'PRINCIPAL_ONLY' | 'NO_ROLLOVER';
  accruedInterest: number;
  startDate: string;
  maturityDate: string;
  status: 'ACTIVE' | 'MATURED' | 'CLOSED_PREMATURE';
}

export interface LoanDto {
  id: string;
  accountId: string;
  loanNumber: string;
  principalAmount: number;
  interestRate: number;
  termMonths: number;
  remainingPrincipal: number;
  status: 'DISBURSED' | 'ACTIVE' | 'PAID_OFF' | 'DEFAULTED';
  disbursedAt: string;
}

export interface EodReportDto {
  id: string;
  reconciliationDate: string;
  totalAccountsChecked: number;
  totalAccountBalance: number;
  totalLockedBalance: number;
  totalAvailableBalance: number;
  totalLedgerDebits: number;
  totalLedgerCredits: number;
  ledgerBalanced: boolean;
  discrepancyCount: number;
  status: 'BALANCED' | 'DISCREPANCY_FOUND' | 'FAILED';
  executionDurationMs: number;
  createdAt: string;
}

export interface SimulationProgressDto {
  status: 'IDLE' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  currentDay: number;
  totalDays: number;
  progressPercent: number;
  totalTransactions: number;
  totalLedgerEntries: number;
  totalSavingsAccounts: number;
  totalLoansDisbursed: number;
  executionDurationMs: number;
  message: string;
}

export interface SimulationResultDto {
  status: string;
  userCount: number;
  daysSimulated: number;
  totalTransactions: number;
  totalLedgerEntries: number;
  totalTransferVolume: number;
  totalFeesCollected: number;
  totalSavingsOpened: number;
  totalSavingsPrincipal: number;
  totalInterestExpense: number;
  totalLoansDisbursed: number;
  totalLoanVolume: number;
  totalInterestIncome: number;
  allEodReconciled: boolean;
  totalDurationMs: number;
}

export interface TransactionFrequencyPoint {
  timeLabel: string;
  transactionCount: number;
  volume: number;
  successCount: number;
  failedCount: number;
}
