import React, { useState, useMemo } from 'react';
import { Search, BookOpen, ArrowRight } from 'lucide-react';
import { Transaction } from '../../core/types/banking';
import { formatCurrency, formatDateTime, truncateId } from '../../core/utils/formatters';
import { Badge } from '../../components/ui/Badge';
import { DoubleEntryDrawer } from './DoubleEntryDrawer';

interface TransactionManagementTableProps {
  transactions: Transaction[];
  onRefresh?: () => void;
}

export const TransactionManagementTable: React.FC<TransactionManagementTableProps> = ({
  transactions,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [selectedTx, setSelectedTx] = useState<Transaction | null>(null);

  const filteredTransactions = useMemo(() => {
    return transactions.filter((tx) => {
      const matchesStatus = statusFilter === 'ALL' || tx.status === statusFilter;
      const term = searchTerm.toLowerCase();
      const matchesSearch =
        !searchTerm ||
        tx.id.toLowerCase().includes(term) ||
        tx.sourceAccountId.toLowerCase().includes(term) ||
        tx.destinationAccountId.toLowerCase().includes(term) ||
        tx.idempotencyKey.toLowerCase().includes(term);
      return matchesStatus && matchesSearch;
    });
  }, [transactions, statusFilter, searchTerm]);

  const statuses: { label: string; value: string }[] = [
    { label: 'Tất cả (All)', value: 'ALL' },
    { label: 'Hoàn tất (Completed)', value: 'COMPLETED' },
    { label: 'Hoàn trả (Compensated)', value: 'COMPENSATED' },
    { label: 'Thất bại (Failed)', value: 'FAILED' },
  ];

  return (
    <div className="glass-panel p-5 rounded-xl border border-slate-800/80 space-y-4">
      {/* Header & Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 border-b border-slate-800/60 pb-4">
        <div>
          <h3 className="text-sm font-semibold text-slate-100 flex items-center gap-2">
            Quản Lý & Kiểm Tra Giao Dịch (Transaction Audit Console)
            <span className="text-xs font-mono text-slate-400 font-normal">
              ({filteredTransactions.length} / {transactions.length} giao dịch)
            </span>
          </h3>
          <p className="text-xs text-slate-400">Tra cứu chi tiết từng lệnh chuyển tiền và soi chiếu bút toán kế toán</p>
        </div>

        {/* Search & Filter */}
        <div className="flex flex-wrap items-center gap-2.5">
          <div className="relative w-64">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Tìm Tx ID, Account, IdemKey..."
              className="w-full bg-slate-900/80 border border-slate-800 rounded-lg pl-9 pr-3 py-1.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-indigo-500/60 transition-colors"
            />
          </div>

          {/* Filter Pills */}
          <div className="flex items-center gap-1 p-1 rounded-lg bg-slate-900 border border-slate-800 text-xs">
            {statuses.map((st) => (
              <button
                key={st.value}
                onClick={() => setStatusFilter(st.value)}
                className={`px-2.5 py-1 rounded-md text-[11px] font-medium transition-colors ${
                  statusFilter === st.value
                    ? 'bg-indigo-600 text-white shadow-sm'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {st.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Table */}
      <div className="w-full overflow-x-auto rounded-xl border border-slate-800/80 bg-slate-950/40">
        <table className="w-full text-left text-xs text-slate-300">
          <thead className="bg-slate-900/80 text-[11px] uppercase tracking-wider text-slate-400 font-mono border-b border-slate-800">
            <tr>
              <th className="px-4 py-3 font-semibold">Thời gian</th>
              <th className="px-4 py-3 font-semibold">Transaction ID</th>
              <th className="px-4 py-3 font-semibold">Nguồn → Đích</th>
              <th className="px-4 py-3 font-semibold text-right">Số tiền</th>
              <th className="px-4 py-3 font-semibold">Trạng thái</th>
              <th className="px-4 py-3 font-semibold">Saga Step</th>
              <th className="px-4 py-3 font-semibold text-center">Bút toán kép</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60 font-sans">
            {filteredTransactions.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-slate-500 text-xs">
                  Không tìm thấy giao dịch nào phù hợp với bộ lọc tìm kiếm.
                </td>
              </tr>
            ) : (
              filteredTransactions.map((tx) => (
                <tr
                  key={tx.id}
                  className="hover:bg-slate-800/30 transition-colors duration-150 group"
                >
                  <td className="px-4 py-3 font-mono text-slate-400 whitespace-nowrap">
                    {formatDateTime(tx.createdAt)}
                  </td>
                  <td className="px-4 py-3 font-mono text-slate-200">
                    <span title={tx.id}>{truncateId(tx.id, 6, 4)}</span>
                  </td>
                  <td className="px-4 py-3 font-mono text-slate-300">
                    <div className="flex items-center gap-1.5">
                      <span className="text-slate-400" title={tx.sourceAccountId}>
                        {truncateId(tx.sourceAccountId, 5, 3)}
                      </span>
                      <ArrowRight className="w-3 h-3 text-slate-600" />
                      <span className="text-indigo-300" title={tx.destinationAccountId}>
                        {truncateId(tx.destinationAccountId, 5, 3)}
                      </span>
                    </div>
                  </td>
                  <td className="px-4 py-3 font-mono text-right font-bold text-white whitespace-nowrap">
                    {formatCurrency(tx.amount, tx.currency)}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
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
                  </td>
                  <td className="px-4 py-3 font-mono text-[11px] text-indigo-400 whitespace-nowrap">
                    {tx.sagaStep}
                  </td>
                  <td className="px-4 py-3 text-center whitespace-nowrap">
                    <button
                      onClick={() => setSelectedTx(tx)}
                      className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-indigo-600/10 hover:bg-indigo-600/20 text-indigo-300 border border-indigo-500/20 hover:border-indigo-500/40 text-[11px] font-medium transition-all"
                    >
                      <BookOpen className="w-3.5 h-3.5" />
                      <span>Audit Ledger</span>
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Double Entry Audit Modal */}
      {selectedTx && (
        <DoubleEntryDrawer
          transaction={selectedTx}
          onClose={() => setSelectedTx(null)}
        />
      )}
    </div>
  );
};
