import React from 'react';
import { SlidingWindowMeter } from '../modules/fraud/SlidingWindowMeter';
import { Card } from '../components/ui/Card';
import { ShieldCheck, Zap, AlertTriangle, Lock } from 'lucide-react';

export const FraudConsole: React.FC = () => {
  return (
    <div className="space-y-6">
      <div>
        <h3 className="text-lg font-bold text-slate-100 tracking-tight">
          Fraud Prevention & Anomaly Mitigation
        </h3>
        <p className="text-xs text-slate-400 mt-0.5">
          Redis Sorted Sets sliding window counter and in-memory anomaly scoring before debit commitment.
        </p>
      </div>

      <SlidingWindowMeter />

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <Card header={<h4 className="text-xs font-semibold text-slate-100 uppercase tracking-wider">Active Fraud Defense Rules</h4>}>
          <div className="space-y-3 text-xs">
            <div className="flex items-start gap-3 p-3 rounded-lg bg-slate-900/60 border border-slate-800">
              <Zap className="w-4 h-4 text-indigo-400 shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold text-slate-200">Sliding Window Velocity Rule</p>
                <p className="text-slate-400 text-[11px] mt-0.5">
                  Allows maximum 5 requests in any 60-second moving window per account (`ZREMRANGEBYSCORE`).
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3 p-3 rounded-lg bg-slate-900/60 border border-slate-800">
              <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold text-slate-200">Abnormal Transaction Amount Rule</p>
                <p className="text-slate-400 text-[11px] mt-0.5">
                  Flags transactions strictly exceeding $100,000 for secondary risk assessment.
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3 p-3 rounded-lg bg-slate-900/60 border border-slate-800">
              <Lock className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold text-slate-200">High-Risk Hours Rule</p>
                <p className="text-slate-400 text-[11px] mt-0.5">
                  Transactions between 00:00 - 05:00 UTC face reduced threshold limits.
                </p>
              </div>
            </div>
          </div>
        </Card>

        <Card header={<h4 className="text-xs font-semibold text-slate-100 uppercase tracking-wider">Compensator Protocol</h4>}>
          <div className="space-y-3 text-xs text-slate-300 leading-relaxed">
            <p>
              When a transaction is rejected by the Fraud Engine:
            </p>
            <ol className="list-decimal pl-4 space-y-1.5 text-slate-400">
              <li>Saga step halts immediately before the external interbank switch.</li>
              <li>Compensator triggers <code className="text-indigo-300 font-mono">AccountService.releaseHeldFunds()</code>.</li>
              <li>Locked balance is deducted and returned to available balance.</li>
              <li>Transaction status is transitioned to <code className="text-amber-300 font-mono">COMPENSATED</code> with audit reason.</li>
            </ol>
            <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-lg text-emerald-300 flex items-center gap-2">
              <ShieldCheck className="w-4 h-4 shrink-0" />
              <span>Zero funds leakage or stranded held balances.</span>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
};
