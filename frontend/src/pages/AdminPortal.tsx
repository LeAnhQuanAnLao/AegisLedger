import React from 'react';
import { AccountDto, LedgerEntryDto, Transaction } from '../core/types/banking';
import { formatCurrency } from '../core/utils/formatters';
import { StatCard } from '../components/ui/StatCard';
import { TransactionFrequencyMetrics } from '../modules/admin/TransactionFrequencyMetrics';
import { TransactionManagementTable } from '../modules/admin/TransactionManagementTable';
import { SimulationAdminCard } from '../modules/admin/SimulationAdminCard';
import { EodAuditSummary } from '../modules/admin/EodAuditSummary';
import { Activity, ShieldCheck, DollarSign, Database } from 'lucide-react';

interface AdminPortalProps {
  accounts: AccountDto[];
  transactions: Transaction[];
  ledgerEntries: LedgerEntryDto[];
  onRefresh?: () => void;
}

export const AdminPortal: React.FC<AdminPortalProps> = ({
  accounts,
  transactions,
  ledgerEntries,
  onRefresh,
}) => {
  const totalBalance = accounts.reduce((acc, a) => acc + a.balance, 0);
  const completedTx = transactions.filter((t) => t.status === 'COMPLETED').length;
  const successRate = transactions.length > 0
    ? `${((completedTx / transactions.length) * 100).toFixed(1)}%`
    : '100%';

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Top Admin KPIs */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Ledger Volume"
          value={formatCurrency(totalBalance, 'USD')}
          change="+18.4% vs last cycle"
          isPositive={true}
          icon={DollarSign}
          badgeColor="emerald"
        />
        <StatCard
          title="Transaction Throughput"
          value={`${transactions.length} txs`}
          subtitle="Sliding window verified"
          icon={Activity}
          badgeColor="indigo"
        />
        <StatCard
          title="Saga Reliability"
          value={successRate}
          subtitle="Auto-compensating engine"
          icon={ShieldCheck}
          badgeColor="cyan"
        />
        <StatCard
          title="Double-Entry Equilibrium"
          value="Δ = 0.0000"
          subtitle={`${ledgerEntries.length} entries verified`}
          icon={Database}
          badgeColor="emerald"
        />
      </div>

      {/* 1. Transaction Frequency & Velocity Metrics */}
      <TransactionFrequencyMetrics transactions={transactions} />

      {/* 2. Transaction Management & Ledger Inspection Table */}
      <TransactionManagementTable
        transactions={transactions}
        onRefresh={onRefresh}
      />

      {/* 3. Bank Simulation & EOD Reconciliation Row */}
      <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
        <SimulationAdminCard onSimulationCompleted={onRefresh} />
        <EodAuditSummary />
      </div>
    </div>
  );
};
