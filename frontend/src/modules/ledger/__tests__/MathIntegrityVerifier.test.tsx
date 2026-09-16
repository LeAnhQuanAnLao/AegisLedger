import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MathIntegrityVerifier } from '../MathIntegrityVerifier';
import { LedgerEntryDto } from '../../../core/types/banking';

describe('MathIntegrityVerifier component', () => {
  it('renders balanced status when total debit equals total credit', () => {
    const balancedEntries: LedgerEntryDto[] = [
      {
        id: '1',
        transactionId: 'tx-1',
        accountId: 'acc-1',
        entryType: 'DEBIT',
        amount: 500,
        balanceAfter: 1500,
        description: 'Debit',
        createdAt: new Date().toISOString(),
      },
      {
        id: '2',
        transactionId: 'tx-1',
        accountId: 'acc-2',
        entryType: 'CREDIT',
        amount: 500,
        balanceAfter: 2500,
        description: 'Credit',
        createdAt: new Date().toISOString(),
      },
    ];

    render(<MathIntegrityVerifier entries={balancedEntries} />);
    expect(screen.getByText('Immutable Double-Entry Balanced')).toBeInTheDocument();
    expect(screen.getByText('$0.0000')).toBeInTheDocument();
  });

  it('detects discrepancy and displays warning when entries are unbalanced', () => {
    const unBalancedEntries: LedgerEntryDto[] = [
      {
        id: '1',
        transactionId: 'tx-1',
        accountId: 'acc-1',
        entryType: 'DEBIT',
        amount: 500,
        balanceAfter: 1500,
        description: 'Debit',
        createdAt: new Date().toISOString(),
      },
    ];

    render(<MathIntegrityVerifier entries={unBalancedEntries} />);
    expect(screen.getByText('CRITICAL INTEGRITY MISMATCH')).toBeInTheDocument();
    expect(screen.getByText('$500.0000')).toBeInTheDocument();
  });
});
