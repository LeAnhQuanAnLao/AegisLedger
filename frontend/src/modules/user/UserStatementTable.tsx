import React, { useState, useEffect } from 'react';
import { FileText, ArrowDownLeft, ArrowUpRight, Search } from 'lucide-react';
import { LedgerEntryDto } from '../../core/types/banking';
import { ledgerApi } from '../../core/api/httpClient';
import { formatCurrency, formatDateTime, truncateId } from '../../core/utils/formatters';

interface UserStatementTableProps {
  accountId: string;
}

export const UserStatementTable: React.FC<UserStatementTableProps> = ({ accountId }) => {
  const [entries, setEntries] = useState<LedgerEntryDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    if (!accountId) return;
    setLoading(true);
    ledgerApi.getAccountEntries(accountId, 0, 50)
      .then((res) => {
        setEntries(res.data?.content || []);
      })
      .catch((err) => console.error('Failed to load user statement:', err))
      .finally(() => setLoading(false));
  }, [accountId]);

  const filteredEntries = entries.filter((e) => {
    if (!searchTerm) return true;
    const term = searchTerm.toLowerCase();
    return (
      e.description.toLowerCase().includes(term) ||
      e.transactionId.toLowerCase().includes(term) ||
      e.entryType.toLowerCase().includes(term)
    );
  });

  return (
    <div className="glass-panel p-5 rounded-xl border border-slate-800/80 space-y-4">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-800/60 pb-3">
        <div className="flex items-center gap-2.5">
          <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            <FileText className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-sm font-semibold text-slate-100 flex items-center gap-2">
              Bảng Sao Kê Biến Động Số Dư (Account Statement)
              <span className="text-xs font-mono text-slate-400 font-normal">
                ({filteredEntries.length} biến động)
              </span>
            </h3>
            <p className="text-xs text-slate-400">Lịch sử giao dịch sổ cái chi tiết với số dư tức thời sau mỗi giao dịch</p>
          </div>
        </div>

        {/* Search */}
        <div className="relative w-full sm:w-64">
          <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Tìm theo nội dung, Tx ID..."
            className="w-full bg-slate-900 border border-slate-800 rounded-lg pl-8 pr-3 py-1.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-indigo-500"
          />
        </div>
      </div>

      {/* Table */}
      <div className="w-full overflow-x-auto rounded-xl border border-slate-800/80 bg-slate-950/40">
        <table className="w-full text-left text-xs text-slate-300">
          <thead className="bg-slate-900/80 text-[11px] uppercase tracking-wider text-slate-400 font-mono border-b border-slate-800">
            <tr>
              <th className="px-4 py-3 font-semibold">Thời gian</th>
              <th className="px-4 py-3 font-semibold">Loại</th>
              <th className="px-4 py-3 font-semibold">Nội dung giao dịch</th>
              <th className="px-4 py-3 font-semibold text-right">Biến động số tiền</th>
              <th className="px-4 py-3 font-semibold text-right">Số dư sau GD</th>
              <th className="px-4 py-3 font-semibold text-center">Tx Reference</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60 font-sans">
            {loading ? (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-slate-500 text-xs font-mono animate-pulse">
                  Đang tải dữ liệu sao kê sổ cái kép...
                </td>
              </tr>
            ) : filteredEntries.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-slate-500 text-xs">
                  Chưa có giao dịch biến động nào cho tài khoản này.
                </td>
              </tr>
            ) : (
              filteredEntries.map((item) => {
                const isCredit = item.entryType === 'CREDIT';

                return (
                  <tr key={item.id} className="hover:bg-slate-800/30 transition-colors">
                    <td className="px-4 py-3 font-mono text-slate-400 whitespace-nowrap">
                      {formatDateTime(item.createdAt)}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      <span
                        className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-mono font-bold ${
                          isCredit
                            ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                            : 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                        }`}
                      >
                        {isCredit ? (
                          <>
                            <ArrowDownLeft className="w-3 h-3 text-emerald-400" />
                            CÓ (IN)
                          </>
                        ) : (
                          <>
                            <ArrowUpRight className="w-3 h-3 text-rose-400" />
                            NỢ (OUT)
                          </>
                        )}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-slate-200">
                      <p className="font-medium text-xs">{item.description}</p>
                    </td>
                    <td
                      className={`px-4 py-3 font-mono text-right font-bold whitespace-nowrap ${
                        isCredit ? 'text-emerald-400' : 'text-rose-400'
                      }`}
                    >
                      {isCredit ? '+' : '-'}{formatCurrency(item.amount, 'USD')}
                    </td>
                    <td className="px-4 py-3 font-mono text-right font-bold text-white whitespace-nowrap">
                      {formatCurrency(item.balanceAfter, 'USD')}
                    </td>
                    <td className="px-4 py-3 text-center font-mono text-slate-500 text-[11px]">
                      <span title={item.transactionId}>{truncateId(item.transactionId, 6, 4)}</span>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
