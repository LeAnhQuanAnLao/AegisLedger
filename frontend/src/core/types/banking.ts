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
