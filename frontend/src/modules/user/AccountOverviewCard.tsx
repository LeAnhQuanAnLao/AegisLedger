import React from 'react';
import { CreditCard, Wallet, Lock, ShieldAlert, ArrowUpRight } from 'lucide-react';
import { AccountDto, DailyLimitStatusDto } from '../../core/types/banking';
import { formatCurrency } from '../../core/utils/formatters';
import { Button } from '../../components/ui/Button';

interface AccountOverviewCardProps {
  accounts: AccountDto[];
  selectedAccountId: string;
  onSelectAccount: (id: string) => void;
  limitStatus: DailyLimitStatusDto | null;
  onOpenTransfer: () => void;
}

export const AccountOverviewCard: React.FC<AccountOverviewCardProps> = ({
  accounts,
  selectedAccountId,
  onSelectAccount,
  limitStatus,
  onOpenTransfer,
}) => {
  const currentAccount = accounts.find((a) => a.id === selectedAccountId) || accounts[0];

  const configuredLimit = limitStatus?.configuredLimit || 50000;
  const spentToday = limitStatus?.totalSpentToday || 0;
  const remainingLimit = limitStatus?.remainingLimit || (configuredLimit - spentToday);
  const limitUsagePercent = Math.min(100, Math.round((spentToday / configuredLimit) * 100));

  return (
    <div className="glass-panel p-6 rounded-2xl border border-slate-800/80 space-y-6">
      {/* Top: Account Switcher & Details */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800/60 pb-5">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-indigo-500 to-indigo-700 flex items-center justify-center text-white shadow-glow-indigo">
            <CreditCard className="w-6 h-6" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-lg font-bold text-white tracking-tight">
                {currentAccount ? currentAccount.holderName : 'Khách hàng cá nhân'}
              </h2>
              <span className="text-[11px] font-mono px-2 py-0.5 rounded-md bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-semibold">
                ACTIVE
              </span>
            </div>
            <p className="text-xs text-slate-400 font-mono">
              Số tài khoản: <span className="text-slate-200 font-bold">{currentAccount?.accountNumber}</span>
            </p>
          </div>
        </div>

        {/* Account Switcher Dropdown */}
        <div className="flex items-center gap-3">
          <div className="flex flex-col">
            <label className="text-[10px] font-mono uppercase text-slate-400 mb-1">
              Đổi tài khoản đăng nhập (Account Switcher)
            </label>
            <select
              value={selectedAccountId}
              onChange={(e) => onSelectAccount(e.target.value)}
              className="bg-slate-900 border border-slate-700/80 text-xs font-mono text-white rounded-lg px-3 py-1.5 focus:outline-none focus:border-indigo-500"
            >
              {accounts.map((acc) => (
                <option key={acc.id} value={acc.id}>
                  {acc.holderName} ({acc.accountNumber})
                </option>
              ))}
            </select>
          </div>

          <Button
            onClick={onOpenTransfer}
            className="bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold px-4 py-2 mt-3 sm:mt-0"
          >
            <ArrowUpRight className="w-4 h-4 mr-1.5" />
            Chuyển tiền
          </Button>
        </div>
      </div>

      {/* Balance Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Available Balance */}
        <div className="p-4 rounded-xl bg-gradient-to-br from-indigo-950/40 to-slate-900/60 border border-indigo-500/30 space-y-1">
          <div className="flex items-center justify-between text-xs text-indigo-300 font-mono">
            <span className="flex items-center gap-1.5">
              <Wallet className="w-3.5 h-3.5 text-indigo-400" />
              Số dư khả dụng (Available)
            </span>
          </div>
          <p className="text-2xl font-bold font-mono text-white">
            {formatCurrency(currentAccount?.availableBalance || 0, currentAccount?.currency || 'USD')}
          </p>
          <p className="text-[11px] text-slate-400 font-mono">Có thể thực hiện chuyển tiền ngay</p>
        </div>

        {/* Locked Balance */}
        <div className="p-4 rounded-xl bg-slate-900/50 border border-slate-800/80 space-y-1">
          <div className="flex items-center justify-between text-xs text-slate-400 font-mono">
            <span className="flex items-center gap-1.5">
              <Lock className="w-3.5 h-3.5 text-amber-400" />
              Số dư tạm giữ (Locked)
            </span>
          </div>
          <p className="text-2xl font-bold font-mono text-amber-400">
            {formatCurrency(currentAccount?.lockedBalance || 0, currentAccount?.currency || 'USD')}
          </p>
          <p className="text-[11px] text-slate-500 font-mono">Đang trong tiến trình Saga / Bảo lãnh</p>
        </div>

        {/* Total Ledger Balance */}
        <div className="p-4 rounded-xl bg-slate-900/50 border border-slate-800/80 space-y-1">
          <div className="flex items-center justify-between text-xs text-slate-400 font-mono">
            <span>Tổng số dư ghi sổ (Total Balance)</span>
          </div>
          <p className="text-2xl font-bold font-mono text-slate-200">
            {formatCurrency(currentAccount?.balance || 0, currentAccount?.currency || 'USD')}
          </p>
          <p className="text-[11px] text-slate-500 font-mono">Khả dụng + Tạm giữ</p>
        </div>
      </div>

      {/* Daily Spending Limit Progress Bar */}
      <div className="p-4 rounded-xl bg-slate-900/40 border border-slate-800/60 space-y-2">
        <div className="flex items-center justify-between text-xs font-mono">
          <span className="text-slate-400 flex items-center gap-1.5">
            <ShieldAlert className="w-3.5 h-3.5 text-indigo-400" />
            Hạn mức chuyển tiền trong ngày:
            <span className="text-white font-bold">{formatCurrency(configuredLimit, 'USD')}</span>
          </span>
          <span className="text-slate-300">
            Đã dùng: <strong className="text-indigo-400">{formatCurrency(spentToday, 'USD')}</strong> (còn lại {formatCurrency(remainingLimit, 'USD')})
          </span>
        </div>
        <div className="w-full h-2 rounded-full bg-slate-950 border border-slate-800 overflow-hidden">
          <div
            className={`h-full rounded-full transition-all duration-300 ${
              limitUsagePercent > 80
                ? 'bg-rose-500'
                : limitUsagePercent > 50
                ? 'bg-amber-400'
                : 'bg-indigo-500'
            }`}
            style={{ width: `${limitUsagePercent}%` }}
          />
        </div>
      </div>
    </div>
  );
};
