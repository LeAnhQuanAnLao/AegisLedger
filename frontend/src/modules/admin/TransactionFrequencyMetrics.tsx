import React, { useMemo } from 'react';
import { Activity, Zap, TrendingUp, AlertTriangle } from 'lucide-react';
import { Transaction, TransactionFrequencyPoint } from '../../core/types/banking';
import { analyticsApi } from '../../core/api/bankingServicesApi';
import { formatCurrency } from '../../core/utils/formatters';

interface TransactionFrequencyMetricsProps {
  transactions: Transaction[];
}

export const TransactionFrequencyMetrics: React.FC<TransactionFrequencyMetricsProps> = ({
  transactions,
}) => {
  const frequencyPoints: TransactionFrequencyPoint[] = useMemo(() => {
    return analyticsApi.getFrequencyTimeline(transactions);
  }, [transactions]);

  const totalTxCount = transactions.length;
  const completedCount = transactions.filter((t) => t.status === 'COMPLETED').length;
  const failedOrRollbackCount = totalTxCount - completedCount;
  const successRate = totalTxCount > 0 ? ((completedCount / totalTxCount) * 100).toFixed(1) : '100.0';
  const totalVolume = transactions.reduce((acc, t) => acc + t.amount, 0);

  // Approximate TPM / TPS
  const estTpm = Math.max(12, Math.round(totalTxCount * 2.4));
  const estTps = (estTpm / 60).toFixed(1);

  const maxBucketCount = Math.max(...frequencyPoints.map((p) => p.transactionCount), 1);

  return (
    <div className="glass-panel p-5 rounded-xl border border-slate-800/80 space-y-5">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-800/60 pb-3">
        <div className="flex items-center gap-2.5">
          <div className="p-2 rounded-lg bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
            <Activity className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-sm font-semibold text-slate-100 flex items-center gap-2">
              Transaction Velocity & Frequency Analytics
              <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                REAL-TIME
              </span>
            </h3>
            <p className="text-xs text-slate-400">Tần suất giao dịch phân bố theo thời gian và lưu lượng thanh toán</p>
          </div>
        </div>

        {/* Velocity Indicator */}
        <div className="flex items-center gap-3 text-xs font-mono">
          <div className="px-3 py-1 rounded-lg bg-slate-900 border border-slate-800 flex items-center gap-2">
            <Zap className="w-3.5 h-3.5 text-amber-400 animate-pulse" />
            <span className="text-slate-400">TPS:</span>
            <span className="font-bold text-slate-200">{estTps}</span>
          </div>
          <div className="px-3 py-1 rounded-lg bg-slate-900 border border-slate-800 flex items-center gap-2">
            <TrendingUp className="w-3.5 h-3.5 text-indigo-400" />
            <span className="text-slate-400">TPM:</span>
            <span className="font-bold text-slate-200">{estTpm}</span>
          </div>
        </div>
      </div>

      {/* Metric Highlights */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <div className="p-3 rounded-lg bg-slate-900/50 border border-slate-800/60 space-y-1">
          <p className="text-[11px] font-mono text-slate-400">Total Volume</p>
          <p className="text-base font-bold font-mono text-white">{formatCurrency(totalVolume, 'USD')}</p>
        </div>
        <div className="p-3 rounded-lg bg-slate-900/50 border border-slate-800/60 space-y-1">
          <p className="text-[11px] font-mono text-slate-400">Success Rate</p>
          <p className="text-base font-bold font-mono text-emerald-400">{successRate}%</p>
        </div>
        <div className="p-3 rounded-lg bg-slate-900/50 border border-slate-800/60 space-y-1">
          <p className="text-[11px] font-mono text-slate-400">Total Operations</p>
          <p className="text-base font-bold font-mono text-slate-200">{totalTxCount} txs</p>
        </div>
        <div className="p-3 rounded-lg bg-slate-900/50 border border-slate-800/60 space-y-1">
          <p className="text-[11px] font-mono text-slate-400">Rollback/Compensated</p>
          <p className="text-base font-bold font-mono text-rose-400">{failedOrRollbackCount}</p>
        </div>
      </div>

      {/* Frequency Distribution Bar Chart */}
      <div className="space-y-2">
        <div className="flex items-center justify-between text-xs text-slate-400 font-mono">
          <span>Khung giờ giao dịch (24h Activity Distribution)</span>
          <span>Số lệnh (Tx Count) / Khối lượng ($)</span>
        </div>

        <div className="grid grid-cols-4 md:grid-cols-8 gap-2 pt-2">
          {frequencyPoints.map((point) => {
            const heightPercent = Math.max(15, Math.round((point.transactionCount / maxBucketCount) * 100));
            const isPeak = heightPercent > 75;

            return (
              <div key={point.timeLabel} className="flex flex-col items-center gap-1.5 group">
                <div className="text-[10px] font-mono text-slate-400 opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap">
                  {point.transactionCount} txs
                </div>

                <div className="w-full h-32 bg-slate-900/70 rounded-lg p-1 flex flex-col justify-end border border-slate-800/60 relative overflow-hidden group-hover:border-indigo-500/40">
                  <div
                    style={{ height: `${heightPercent}%` }}
                    className={`w-full rounded transition-all duration-300 ${
                      isPeak
                        ? 'bg-gradient-to-t from-indigo-600 to-indigo-400 shadow-glow-indigo'
                        : 'bg-gradient-to-t from-slate-700 to-slate-500 group-hover:from-indigo-600/70 group-hover:to-indigo-400/70'
                    }`}
                  />
                  {isPeak && (
                    <div className="absolute top-1 right-1 text-[9px] font-mono text-indigo-300 font-semibold px-1 rounded bg-indigo-950/80 border border-indigo-500/40">
                      PEAK
                    </div>
                  )}
                </div>

                <span className="text-[10px] font-mono text-slate-400 text-center truncate w-full">
                  {point.timeLabel}
                </span>
                <span className="text-[9px] font-mono text-slate-500">
                  ${(point.volume / 1000).toFixed(0)}k
                </span>
              </div>
            );
          })}
        </div>
      </div>

      {/* Safety Notice */}
      <div className="flex items-center gap-2 p-2.5 rounded-lg bg-indigo-950/30 border border-indigo-800/40 text-xs text-slate-300">
        <AlertTriangle className="w-4 h-4 text-indigo-400 shrink-0" />
        <span>
          Tần suất giao dịch được bảo vệ bởi Sliding Window Rate Limiter (tối đa 5 txs/60s) và khóa bi quan (Pessimistic Lock order-by-UUID) triệt tiêu deadlock.
        </span>
      </div>
    </div>
  );
};
