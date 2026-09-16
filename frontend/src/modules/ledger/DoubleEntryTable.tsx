import React, { useState } from 'react';
import { LedgerEntryDto } from '../../core/types/banking';
import { formatCurrency, formatDateTime, truncateId } from '../../core/utils/formatters';
import { Table } from '../../components/ui/Table';
import { Badge } from '../../components/ui/Badge';
import { Search } from 'lucide-react';

interface DoubleEntryTableProps {
  entries: LedgerEntryDto[];
}

export const DoubleEntryTable: React.FC<DoubleEntryTableProps> = ({ entries }) => {
  const [filterType, setFilterType] = useState<'ALL' | 'DEBIT' | 'CREDIT'>('ALL');
  const [searchTerm, setSearchTerm] = useState('');

  const filtered = entries.filter((e) => {
    const matchesType = filterType === 'ALL' || e.entryType === filterType;
    const matchesSearch =
      e.transactionId.toLowerCase().includes(searchTerm.toLowerCase()) ||
      e.accountId.toLowerCase().includes(searchTerm.toLowerCase()) ||
      e.description.toLowerCase().includes(searchTerm.toLowerCase());
    return matchesType && matchesSearch;
  });

  const columns = [
    {
      header: 'Timestamp',
      className: 'w-44',
      render: (e: LedgerEntryDto) => (
        <span className="font-mono text-xs text-slate-400">
          {formatDateTime(e.createdAt)}
        </span>
      ),
    },
    {
      header: 'Entry Type',
      className: 'w-28',
      render: (e: LedgerEntryDto) => (
        <Badge variant={e.entryType === 'CREDIT' ? 'success' : 'danger'}>
          {e.entryType}
        </Badge>
      ),
    },
    {
      header: 'Amount',
      className: 'w-36 font-mono text-right',
      render: (e: LedgerEntryDto) => (
        <span
          className={`font-semibold font-mono ${
            e.entryType === 'CREDIT' ? 'text-emerald-400' : 'text-rose-400'
          }`}
        >
          {e.entryType === 'CREDIT' ? '+' : '-'}
          {formatCurrency(e.amount, 'USD')}
        </span>
      ),
    },
    {
      header: 'Balance After',
      className: 'w-36 font-mono text-right',
      render: (e: LedgerEntryDto) => (
        <span className="font-mono text-slate-300">
          {formatCurrency(e.balanceAfter, 'USD')}
        </span>
      ),
    },
    {
      header: 'Tx UUID',
      className: 'w-36 font-mono',
      render: (e: LedgerEntryDto) => (
        <span className="font-mono text-xs text-slate-400" title={e.transactionId}>
          {truncateId(e.transactionId, 6, 4)}
        </span>
      ),
    },
    {
      header: 'Description',
      render: (e: LedgerEntryDto) => (
        <span className="text-xs text-slate-300 truncate max-w-xs block">
          {e.description}
        </span>
      ),
    },
  ];

  return (
    <div className="space-y-3">
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3">
        <div className="relative flex-1 max-w-sm w-full">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Filter by Tx ID, account or description..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full bg-slate-900 border border-slate-800 rounded-lg pl-9 pr-3 py-1.5 text-xs text-slate-200 outline-none focus:border-indigo-500"
          />
        </div>

        <div className="flex items-center gap-1.5 self-end sm:self-auto">
          {(['ALL', 'DEBIT', 'CREDIT'] as const).map((type) => (
            <button
              key={type}
              onClick={() => setFilterType(type)}
              className={`px-3 py-1 rounded-lg text-xs font-mono font-semibold transition-all ${
                filterType === type
                  ? 'bg-indigo-600 text-white shadow-glow-indigo'
                  : 'bg-slate-900 text-slate-400 hover:text-slate-200 border border-slate-800'
              }`}
            >
              {type}
            </button>
          ))}
        </div>
      </div>

      <Table
        columns={columns}
        data={filtered}
        keyExtractor={(e) => e.id}
        emptyMessage="No ledger entries recorded yet."
      />
    </div>
  );
};
