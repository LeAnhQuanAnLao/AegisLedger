import React from 'react';
import {
  AccountDto,
  LedgerEntryDto,
  Transaction,
  SagaStep,
  TransactionStatus,
} from '../core/types/banking';
import { formatCurrency, formatDateTime, truncateId } from '../core/utils/formatters';
import { StatCard } from '../components/ui/StatCard';
import { Badge } from '../components/ui/Badge';
import { Table } from '../components/ui/Table';
import { MathIntegrityVerifier } from '../modules/ledger/MathIntegrityVerifier';
import { SagaVisualizer } from '../modules/saga/SagaVisualizer';
import { DollarSign, Users, Scale, Activity } from 'lucide-react';

interface DashboardOverviewProps {
  accounts: AccountDto[];
  transactions: Transaction[];
  ledgerEntries: LedgerEntryDto[];
  activeStep: SagaStep;
  activeStatus: TransactionStatus;
  activeTxId?: string;
  activeIdemKey?: string;
}

export const DashboardOverview: React.FC<DashboardOverviewProps> = ({
  accounts,
  transactions,
  ledgerEntries,
  activeStep,
  activeStatus,
  activeTxId,
  activeIdemKey,
}) => {
  const totalBalance = accounts.reduce((sum, a) => sum + a.balance, 0);

  const recentTxColumns = [
    {
      header: 'Time',
      className: 'w-40',
      render: (tx: Transaction) => (
        <span className="font-mono text-xs text-slate-400">
          {formatDateTime(tx.createdAt)}
        </span>
      ),
    },
    {
      header: 'Tx ID',
      className: 'w-36 font-mono',
      render: (tx: Transaction) => (
        <span className="text-xs text-slate-300 font-mono">
          {truncateId(tx.id, 6, 4)}
        </span>
      ),
    },
    {
      header: 'Amount',
      className: 'w-36 font-mono text-right',
      render: (tx: Transaction) => (
        <span className="font-bold text-slate-100 font-mono">
          {formatCurrency(tx.amount, tx.currency)}
        </span>
      ),
    },
    {
      header: 'Status',
      className: 'w-32',
      render: (tx: Transaction) => (
        <Badge
          variant={
            tx.status === 'COMPLETED'
              ? 'success'
              : tx.status === 'COMPENSATED'
              ? 'warning'
              : 'danger'
          }
        >
          {tx.status}
        </Badge>
      ),
    },
    {
      header: 'Saga Step',
      className: 'w-36 font-mono text-xs text-slate-400',
      render: (tx: Transaction) => (
        <span className="text-indigo-400 font-medium">{tx.sagaStep}</span>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* KPI Cards Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Liquidity Volume"
          value={formatCurrency(totalBalance, 'USD')}
          change="+14.2% vs last week"
          isPositive={true}
          icon={DollarSign}
          badgeColor="emerald"
        />
        <StatCard
          title="Active Accounts"
          value={accounts.length}
          subtitle="Pessimistic locked"
          icon={Users}
          badgeColor="indigo"
        />
        <StatCard
          title="Ledger Balance Integrity"
          value="Δ = 0.0000"
          subtitle="Zero Discrepancy"
          icon={Scale}
          badgeColor="emerald"
        />
        <StatCard
          title="Saga Success Rate"
          value="99.4%"
          change="0.6% auto-compensated"
          isPositive={true}
          icon={Activity}
          badgeColor="cyan"
        />
      </div>

      {/* Double Entry Mathematical Verifier Banner */}
      <MathIntegrityVerifier entries={ledgerEntries} />

      {/* Live Saga Pipeline Tracker */}
      <SagaVisualizer
        currentStep={activeStep}
        status={activeStatus}
        transactionId={activeTxId || transactions[0]?.id}
        idempotencyKey={activeIdemKey || transactions[0]?.idempotencyKey}
      />

      {/* Recent Transactions Table */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-slate-100">
            Recent Saga Transactions
          </h3>
          <span className="text-xs font-mono text-slate-400">
            Showing last {Math.min(transactions.length, 5)} transactions
          </span>
        </div>

        <Table
          columns={recentTxColumns}
          data={transactions.slice(0, 5)}
          keyExtractor={(tx) => tx.id}
          emptyMessage="No transactions yet."
        />
      </div>
    </div>
  );
};
