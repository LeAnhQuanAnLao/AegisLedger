import React from 'react';
import { Card } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { ShieldAlert, Activity, Clock, AlertTriangle } from 'lucide-react';
import { MOCK_FRAUD_TELEMETRY } from '../../core/api/mockData';

export const SlidingWindowMeter: React.FC = () => {
  const telemetry = MOCK_FRAUD_TELEMETRY;
  const percentage = (telemetry.slidingWindowCount / telemetry.slidingWindowMax) * 100;

  return (
    <Card
      header={
        <div className="flex items-center justify-between w-full">
          <div className="flex items-center gap-2">
            <ShieldAlert className="w-4 h-4 text-amber-400" />
            <h4 className="text-sm font-semibold text-slate-100">
              Real-Time Fraud & Anomaly Engine
            </h4>
          </div>
          <Badge variant={telemetry.velocityStatus === 'NORMAL' ? 'success' : 'danger'} pulse>
            {telemetry.velocityStatus}
          </Badge>
        </div>
      }
    >
      <div className="space-y-5">
        {/* Sliding window bar */}
        <div className="space-y-2">
          <div className="flex items-center justify-between text-xs">
            <span className="text-slate-300 font-medium flex items-center gap-1.5">
              <Activity className="w-3.5 h-3.5 text-indigo-400" />
              Redis Sliding Window Velocity
            </span>
            <span className="font-mono font-semibold text-indigo-400">
              {telemetry.slidingWindowCount} / {telemetry.slidingWindowMax} txs ({telemetry.windowSeconds}s)
            </span>
          </div>

          <div className="w-full h-2.5 bg-slate-800/80 rounded-full overflow-hidden p-0.5 border border-slate-700/60">
            <div
              className={`h-full rounded-full transition-all duration-500 ${
                percentage > 80
                  ? 'bg-rose-500'
                  : percentage > 50
                  ? 'bg-amber-500'
                  : 'bg-indigo-500'
              }`}
              style={{ width: `${Math.min(percentage, 100)}%` }}
            />
          </div>
        </div>

        {/* Dynamic rule inspection grid */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div className="p-3 bg-slate-900/60 rounded-xl border border-slate-800 space-y-1">
            <p className="text-[10px] uppercase font-mono text-slate-500">Risk Score</p>
            <p className="text-xl font-bold font-mono text-emerald-400">
              {telemetry.riskScore} <span className="text-xs text-slate-500">/ 100</span>
            </p>
            <p className="text-[11px] text-slate-400">Low Risk Tier</p>
          </div>

          <div className="p-3 bg-slate-900/60 rounded-xl border border-slate-800 space-y-1">
            <p className="text-[10px] uppercase font-mono text-slate-500">Max Single Tx</p>
            <p className="text-xl font-bold font-mono text-slate-200">
              $100K <span className="text-xs text-slate-500">Threshold</span>
            </p>
            <p className="text-[11px] text-emerald-400">Rule Active</p>
          </div>

          <div className="p-3 bg-slate-900/60 rounded-xl border border-slate-800 space-y-1">
            <p className="text-[10px] uppercase font-mono text-slate-500">Night Hours Window</p>
            <div className="flex items-center gap-1.5 pt-1">
              <Clock className="w-4 h-4 text-slate-400" />
              <span className="text-sm font-semibold text-slate-300">00:00 - 05:00 UTC</span>
            </div>
            <p className="text-[11px] text-slate-500">Off-peak multiplier</p>
          </div>
        </div>

        <div className="p-3 rounded-lg bg-slate-900/40 border border-slate-800/80 flex items-start gap-2.5 text-xs text-slate-400">
          <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
          <p className="leading-relaxed">
            Transactions violating velocity rates or abnormal amount thresholds trigger immediate automatic Saga Compensation rollback with zero balance loss.
          </p>
        </div>
      </div>
    </Card>
  );
};
