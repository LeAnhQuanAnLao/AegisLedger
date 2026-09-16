import { SagaStep, TransactionStatus } from '../types/banking';

export interface SagaStepConfig {
  step: SagaStep;
  number: number;
  label: string;
  subTitle: string;
  description: string;
}

export const SAGA_STEPS: SagaStepConfig[] = [
  {
    step: 'STARTED',
    number: 1,
    label: 'Initiate Transfer',
    subTitle: 'Idempotency Acquired',
    description: 'Verifies idempotency key in Redis and initiates transaction record.',
  },
  {
    step: 'FUNDS_HELD',
    number: 2,
    label: 'Pessimistic Hold',
    subTitle: 'SELECT ... FOR UPDATE',
    description: 'Acquires DB lock on source account and holds balance to prevent race conditions.',
  },
  {
    step: 'FRAUD_EVALUATED',
    number: 3,
    label: 'Fraud Assessment',
    subTitle: 'Sliding Window & Velocity',
    description: 'Redis Sorted Set counter inspects sliding window velocity and high-risk rules.',
  },
  {
    step: 'SWITCH_PROCESSED',
    number: 4,
    label: 'External Switch',
    subTitle: 'Partner Interbank Route',
    description: 'Simulates or executes real-time external interbank transfer network switch.',
  },
  {
    step: 'COMMITTED',
    number: 5,
    label: 'Commit Ledger',
    subTitle: 'Strict Double-Entry',
    description: 'Atomically creates balanced DEBIT & CREDIT entries and commits final balances.',
  },
];

export const STATUS_COLORS: Record<TransactionStatus, { badge: string; text: string; bg: string }> = {
  PENDING: { badge: 'warning', text: 'text-amber-400', bg: 'bg-amber-500/10' },
  EXECUTING: { badge: 'info', text: 'text-indigo-400', bg: 'bg-indigo-500/10' },
  COMPLETED: { badge: 'success', text: 'text-emerald-400', bg: 'bg-emerald-500/10' },
  COMPENSATED: { badge: 'warning', text: 'text-amber-300', bg: 'bg-amber-500/10' },
  FAILED: { badge: 'danger', text: 'text-rose-400', bg: 'bg-rose-500/10' },
};
