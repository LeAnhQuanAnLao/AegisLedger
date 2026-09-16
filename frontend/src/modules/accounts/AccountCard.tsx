import React, { useState } from 'react';
import { AccountDto } from '../../core/types/banking';
import { formatCurrency, truncateId } from '../../core/utils/formatters';
import { Badge } from '../../components/ui/Badge';
import { Copy, Check, Lock, ShieldCheck, CreditCard } from 'lucide-react';

interface AccountCardProps {
  account: AccountDto;
  isSelected?: boolean;
  onSelect?: (account: AccountDto) => void;
}

export const AccountCard: React.FC<AccountCardProps> = ({
  account,
  isSelected = false,
  onSelect,
}) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = (e: React.MouseEvent) => {
    e.stopPropagation();
    navigator.clipboard.writeText(account.accountNumber);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div
      onClick={() => onSelect?.(account)}
      className={`relative p-5 rounded-2xl cursor-pointer transition-all duration-300 border ${
        isSelected
          ? 'bg-gradient-to-br from-indigo-950/70 via-slate-900 to-slate-950 border-indigo-500 shadow-glow-indigo'
          : 'bg-gradient-to-br from-slate-900/90 via-slate-900/60 to-fintech-dark border-slate-800 hover:border-slate-700 hover:shadow-lg'
      }`}
    >
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-indigo-500/20 border border-indigo-500/30 flex items-center justify-center text-indigo-400">
            <CreditCard className="w-4 h-4" />
          </div>
          <div>
            <p className="text-xs text-slate-400 font-mono tracking-wider">AEGIS DEBIT</p>
            <p className="text-xs font-semibold text-slate-200 truncate max-w-[160px]">
              {account.holderName}
            </p>
          </div>
        </div>

        <Badge
          variant={account.status === 'ACTIVE' ? 'success' : 'danger'}
          pulse={account.status === 'ACTIVE'}
        >
          {account.status}
        </Badge>
      </div>

      {/* Account Number with Quick Copy */}
      <div className="my-3 flex items-center justify-between bg-slate-950/60 px-3 py-2 rounded-lg border border-slate-800/80">
        <span className="font-mono text-xs tracking-widest text-slate-300">
          {account.accountNumber}
        </span>
        <button
          onClick={handleCopy}
          className="text-slate-400 hover:text-white transition-colors p-1"
          title="Copy Account Number"
        >
          {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
        </button>
      </div>

      {/* Balances Section */}
      <div className="pt-2 border-t border-slate-800/60 flex items-end justify-between">
        <div>
          <p className="text-[11px] text-slate-400 font-medium uppercase">Available Balance</p>
          <p className="text-lg font-bold font-mono text-emerald-400">
            {formatCurrency(account.availableBalance, account.currency)}
          </p>
        </div>

        {account.lockedBalance > 0 && (
          <div className="text-right">
            <div className="flex items-center gap-1 text-[10px] text-amber-400 font-medium justify-end">
              <Lock className="w-3 h-3" />
              <span>Held</span>
            </div>
            <p className="text-xs font-mono text-amber-400/90 font-medium">
              {formatCurrency(account.lockedBalance, account.currency)}
            </p>
          </div>
        )}
      </div>

      <div className="mt-3 text-[10px] text-slate-500 font-mono flex items-center justify-between">
        <span>ID: {truncateId(account.id, 6, 4)}</span>
        <div className="flex items-center gap-1 text-slate-400">
          <ShieldCheck className="w-3 h-3 text-emerald-400" />
          <span>Pessimistic Lock Protected</span>
        </div>
      </div>
    </div>
  );
};
