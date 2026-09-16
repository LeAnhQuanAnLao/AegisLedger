import React from 'react';
import { LedgerEntryDto } from '../core/types/banking';
import { MathIntegrityVerifier } from '../modules/ledger/MathIntegrityVerifier';
import { DoubleEntryTable } from '../modules/ledger/DoubleEntryTable';

interface LedgerExplorerProps {
  entries: LedgerEntryDto[];
}

export const LedgerExplorer: React.FC<LedgerExplorerProps> = ({ entries }) => {
  return (
    <div className="space-y-6">
      <div>
        <h3 className="text-lg font-bold text-slate-100 tracking-tight">
          Immutable Double-Entry Ledger Audit
        </h3>
        <p className="text-xs text-slate-400 mt-0.5">
          Every financial movement strictly produces matched DEBIT and CREDIT entries, ensuring ∑ Debit = ∑ Credit.
        </p>
      </div>

      <MathIntegrityVerifier entries={entries} />

      <div className="glass-panel p-5 rounded-2xl border border-slate-800 space-y-4">
        <h4 className="text-sm font-semibold text-slate-100">
          Append-Only Audit Journal
        </h4>
        <DoubleEntryTable entries={entries} />
      </div>
    </div>
  );
};
