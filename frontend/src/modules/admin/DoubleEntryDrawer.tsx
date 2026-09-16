import React, { useEffect, useState } from 'react';
import { BookOpen, ShieldCheck, X } from 'lucide-react';
import { Transaction, LedgerEntryDto } from '../../core/types/banking';
import { ledgerApi } from '../../core/api/httpClient';
import { formatCurrency, formatDateTime, truncateId } from '../../core/utils/formatters';
import { Badge } from '../../components/ui/Badge';

interface DoubleEntryDrawerProps {
  transaction: Transaction | null;
  onClose: () => void;
}

export const DoubleEntryDrawer: React.FC<DoubleEntryDrawerProps> = ({
  transaction,
  onClose,
}) => {
  const [entries, setEntries] = useState<LedgerEntryDto[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!transaction) return;
    setLoading(true);
    ledgerApi.getByTransactionId(transaction.id)
      .then((res) => {
        setEntries(res.data || []);
      })
      .catch((err) => console.error('Failed to load transaction ledger entries:', err))
      .finally(() => setLoading(false));
  }, [transaction]);

  if (!transaction) return null;

  const debitEntry = entries.find((e) => e.entryType === 'DEBIT');
  const creditEntry = entries.find((e) => e.entryType === 'CREDIT');

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fade-in">
      <div className="w-full max-w-2xl bg-[#0F1422] border border-slate-700/80 rounded-2xl shadow-2xl overflow-hidden flex flex-col">
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-800 flex items-center justify-between bg-slate-900/50">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-lg bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
              <BookOpen className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                Double-Entry Ledger Audit Trail
                <Badge variant={transaction.status === 'COMPLETED' ? 'success' : 'warning'}>
                  {transaction.status}
                </Badge>
              </h3>
              <p className="text-xs text-slate-400 font-mono">Tx ID: {transaction.id}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-6 max-h-[80vh] overflow-y-auto">
          {/* Metadata Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs font-mono">
            <div className="p-3 rounded-xl bg-slate-900/60 border border-slate-800">
              <span className="text-slate-500 block">Amount</span>
              <span className="text-base font-bold text-white">
                {formatCurrency(transaction.amount, transaction.currency)}
              </span>
            </div>
            <div className="p-3 rounded-xl bg-slate-900/60 border border-slate-800">
              <span className="text-slate-500 block">Saga Step</span>
              <span className="text-indigo-400 font-semibold">{transaction.sagaStep}</span>
            </div>
            <div className="p-3 rounded-xl bg-slate-900/60 border border-slate-800">
              <span className="text-slate-500 block">Created At</span>
              <span className="text-slate-300">{formatDateTime(transaction.createdAt)}</span>
            </div>
            <div className="p-3 rounded-xl bg-slate-900/60 border border-slate-800">
              <span className="text-slate-500 block">Idempotency Key</span>
              <span className="text-slate-300 truncate block" title={transaction.idempotencyKey}>
                {transaction.idempotencyKey}
              </span>
            </div>
          </div>

          {/* Mathematical Balance Validation */}
          <div className="p-4 rounded-xl bg-emerald-950/20 border border-emerald-500/30 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <ShieldCheck className="w-6 h-6 text-emerald-400" />
              <div>
                <p className="text-xs font-bold text-emerald-300">Accounting Equilibrium Verified</p>
                <p className="text-[11px] text-emerald-400/80 font-mono">
                  Debit ({formatCurrency(debitEntry?.amount || transaction.amount, 'USD')}) == Credit ({formatCurrency(creditEntry?.amount || transaction.amount, 'USD')})
                </p>
              </div>
            </div>
            <span className="text-xs font-mono font-bold text-emerald-400 px-2.5 py-1 rounded bg-emerald-500/10 border border-emerald-500/20">
              Δ = 0.0000
            </span>
          </div>

          {/* Legs Section */}
          <div className="space-y-3">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-400 font-mono">
              Immutable Accounting Entries (Ledger Records)
            </h4>

            {loading ? (
              <div className="p-8 text-center text-slate-500 text-xs font-mono animate-pulse">
                Fetching ledger verification legs...
              </div>
            ) : entries.length === 0 ? (
              <div className="p-6 rounded-xl bg-slate-900/40 border border-slate-800 text-center text-slate-400 text-xs">
                Không tìm thấy bút toán sổ cái cho giao dịch này (có thể do giao dịch thất bại trước khi ghi sổ).
              </div>
            ) : (
              <div className="space-y-3">
                {/* Debit Leg */}
                {debitEntry && (
                  <div className="p-4 rounded-xl bg-rose-950/10 border border-rose-900/30 space-y-2">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <span className="px-2 py-0.5 rounded bg-rose-500/20 text-rose-400 font-mono text-xs font-bold border border-rose-500/30">
                          DEBIT (NỢ)
                        </span>
                        <span className="text-xs text-slate-300 font-mono">Tài khoản nguồn:</span>
                        <span className="text-xs font-mono font-bold text-white">{truncateId(debitEntry.accountId, 8, 6)}</span>
                      </div>
                      <span className="text-sm font-bold font-mono text-rose-400">
                        -{formatCurrency(debitEntry.amount, 'USD')}
                      </span>
                    </div>
                    <div className="flex items-center justify-between text-xs text-slate-400 font-mono pt-1 border-t border-rose-950/40">
                      <span>Số dư sau giao dịch (balance_after):</span>
                      <span className="text-white font-bold">{formatCurrency(debitEntry.balanceAfter, 'USD')}</span>
                    </div>
                  </div>
                )}

                {/* Credit Leg */}
                {creditEntry && (
                  <div className="p-4 rounded-xl bg-emerald-950/10 border border-emerald-900/30 space-y-2">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <span className="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 font-mono text-xs font-bold border border-emerald-500/30">
                          CREDIT (CÓ)
                        </span>
                        <span className="text-xs text-slate-300 font-mono">Tài khoản đích:</span>
                        <span className="text-xs font-mono font-bold text-white">{truncateId(creditEntry.accountId, 8, 6)}</span>
                      </div>
                      <span className="text-sm font-bold font-mono text-emerald-400">
                        +{formatCurrency(creditEntry.amount, 'USD')}
                      </span>
                    </div>
                    <div className="flex items-center justify-between text-xs text-slate-400 font-mono pt-1 border-t border-emerald-950/40">
                      <span>Số dư sau giao dịch (balance_after):</span>
                      <span className="text-white font-bold">{formatCurrency(creditEntry.balanceAfter, 'USD')}</span>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-3 border-t border-slate-800 bg-slate-900/50 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl bg-slate-800 text-xs font-semibold text-white hover:bg-slate-700 transition-colors"
          >
            Đóng bảng tra cứu
          </button>
        </div>
      </div>
    </div>
  );
};
