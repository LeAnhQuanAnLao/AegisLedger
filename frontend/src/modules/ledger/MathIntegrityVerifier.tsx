import React from 'react';
import { LedgerEntryDto } from '../../core/types/banking';
import { formatCurrency } from '../../core/utils/formatters';
import { ShieldCheck, AlertOctagon } from 'lucide-react';

interface MathIntegrityVerifierProps {
  entries: LedgerEntryDto[];
}

export const MathIntegrityVerifier: React.FC<MathIntegrityVerifierProps> = ({ entries }) => {
  const totalDebit = entries
    .filter((e) => e.entryType === 'DEBIT')
    .reduce((sum, e) => sum + e.amount, 0);

  const totalCredit = entries
    .filter((e) => e.entryType === 'CREDIT')
    .reduce((sum, e) => sum + e.amount, 0);

  const delta = Math.abs(totalDebit - totalCredit);
  const isBalanced = delta < 0.0001;

  return (
    <div
      className={`p-4 rounded-xl border flex flex-col md:flex-row md:items-center justify-between gap-4 transition-all ${
        isBalanced
          ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300 shadow-glow-emerald'
          : 'bg-rose-500/10 border-rose-500/30 text-rose-300 shadow-glow-rose'
      }`}
    >
      <div className="flex items-center gap-3">
        <div
          className={`p-2.5 rounded-lg border ${
            isBalanced
              ? 'bg-emerald-500/20 border-emerald-500/40 text-emerald-400'
              : 'bg-rose-500/20 border-rose-500/40 text-rose-400'
          }`}
        >
          {isBalanced ? <ShieldCheck className="w-5 h-5" /> : <AlertOctagon className="w-5 h-5" />}
        </div>
        <div>
          <div className="flex items-center gap-2">
            <span className="font-semibold text-sm">
              {isBalanced ? 'Immutable Double-Entry Balanced' : 'CRITICAL INTEGRITY MISMATCH'}
            </span>
            <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-slate-900/80 border border-slate-700">
              ∑ Debit = ∑ Credit
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            Every fund transfer strictly creates atomic matching debit and credit entries.
          </p>
        </div>
      </div>

      <div className="flex items-center gap-6 font-mono text-xs">
        <div>
          <p className="text-slate-400 text-[10px] uppercase">Total Debit</p>
          <p className="font-bold text-rose-400">{formatCurrency(totalDebit, 'USD')}</p>
        </div>
        <div className="text-slate-500 text-lg">−</div>
        <div>
          <p className="text-slate-400 text-[10px] uppercase">Total Credit</p>
          <p className="font-bold text-emerald-400">{formatCurrency(totalCredit, 'USD')}</p>
        </div>
        <div className="text-slate-500 text-lg">=</div>
        <div>
          <p className="text-slate-400 text-[10px] uppercase">Discrepancy (Δ)</p>
          <p className={`font-bold ${isBalanced ? 'text-emerald-400' : 'text-rose-400'}`}>
            ${delta.toFixed(4)}
          </p>
        </div>
      </div>
    </div>
  );
};
