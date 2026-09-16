import React, { useState, useEffect } from 'react';
import { PiggyBank, Landmark } from 'lucide-react';
import { SavingsDto, LoanDto } from '../../core/types/banking';
import { savingsApi, loansApi } from '../../core/api/bankingServicesApi';
import { formatCurrency } from '../../core/utils/formatters';
import { Badge } from '../../components/ui/Badge';

interface UserSavingsLoanSummaryProps {
  accountId: string;
}

export const UserSavingsLoanSummary: React.FC<UserSavingsLoanSummaryProps> = ({ accountId }) => {
  const [savings, setSavings] = useState<SavingsDto[]>([]);
  const [loans, setLoans] = useState<LoanDto[]>([]);

  useEffect(() => {
    if (!accountId) return;
    savingsApi.getByAccount(accountId)
      .then((res) => setSavings(res.data || []))
      .catch((e) => console.error('Failed to load savings', e));

    loansApi.getByAccount(accountId)
      .then((res) => setLoans(res.data || []))
      .catch((e) => console.error('Failed to load loans', e));
  }, [accountId]);

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      {/* Savings Accounts Card */}
      <div className="glass-panel p-5 rounded-xl border border-slate-800/80 space-y-4">
        <div className="flex items-center justify-between border-b border-slate-800/60 pb-3">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              <PiggyBank className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-sm font-semibold text-slate-100">Sổ Tiết Kiệm Online (Savings)</h3>
              <p className="text-xs text-slate-400">Lãi dồn tích sinh lời mỗi ngày (Daily Accrual)</p>
            </div>
          </div>
          <span className="text-xs font-mono text-emerald-400 font-bold">{savings.length} sổ</span>
        </div>

        {savings.length === 0 ? (
          <p className="text-xs text-slate-500 py-4 text-center font-mono">Chưa mở sổ tiết kiệm nào.</p>
        ) : (
          <div className="space-y-3">
            {savings.map((s) => (
              <div key={s.id} className="p-3.5 rounded-xl bg-slate-900/50 border border-slate-800 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-mono font-bold text-white">{s.savingsNumber}</span>
                  <Badge variant={s.status === 'ACTIVE' ? 'success' : 'neutral'}>{s.status}</Badge>
                </div>
                <div className="grid grid-cols-3 gap-2 text-xs font-mono pt-1">
                  <div>
                    <span className="text-slate-500 text-[10px] block">Tiền gửi gốc</span>
                    <span className="font-bold text-slate-200">{formatCurrency(s.principalAmount, 'USD')}</span>
                  </div>
                  <div>
                    <span className="text-slate-500 text-[10px] block">Lãi suất</span>
                    <span className="font-bold text-emerald-400">{s.interestRate}%/năm</span>
                  </div>
                  <div>
                    <span className="text-slate-500 text-[10px] block">Lãi dồn tích</span>
                    <span className="font-bold text-indigo-400">+{formatCurrency(s.accruedInterest, 'USD')}</span>
                  </div>
                </div>
                <div className="flex items-center justify-between text-[11px] font-mono text-slate-400 pt-1 border-t border-slate-800/80">
                  <span>Kỳ hạn: {s.termMonths} tháng</span>
                  <span>Đáo hạn: {s.maturityDate}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Micro-Loans Card */}
      <div className="glass-panel p-5 rounded-xl border border-slate-800/80 space-y-4">
        <div className="flex items-center justify-between border-b border-slate-800/60 pb-3">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-lg bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
              <Landmark className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-sm font-semibold text-slate-100">Khoản Vay & Trả Góp (Micro-Lending)</h3>
              <p className="text-xs text-slate-400">Giải ngân tự động & trích nợ tự động (Auto-Debit)</p>
            </div>
          </div>
          <span className="text-xs font-mono text-indigo-400 font-bold">{loans.length} khoản vay</span>
        </div>

        {loans.length === 0 ? (
          <p className="text-xs text-slate-500 py-4 text-center font-mono">Không có khoản vay nào đang hoạt động.</p>
        ) : (
          <div className="space-y-3">
            {loans.map((l) => (
              <div key={l.id} className="p-3.5 rounded-xl bg-slate-900/50 border border-slate-800 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-mono font-bold text-white">{l.loanNumber}</span>
                  <Badge variant={l.status === 'ACTIVE' || l.status === 'DISBURSED' ? 'info' : 'success'}>
                    {l.status}
                  </Badge>
                </div>
                <div className="grid grid-cols-3 gap-2 text-xs font-mono pt-1">
                  <div>
                    <span className="text-slate-500 text-[10px] block">Gốc giải ngân</span>
                    <span className="font-bold text-slate-200">{formatCurrency(l.principalAmount, 'USD')}</span>
                  </div>
                  <div>
                    <span className="text-slate-500 text-[10px] block">Dư nợ còn lại</span>
                    <span className="font-bold text-amber-400">{formatCurrency(l.remainingPrincipal, 'USD')}</span>
                  </div>
                  <div>
                    <span className="text-slate-500 text-[10px] block">Lãi suất vay</span>
                    <span className="font-bold text-slate-300">{l.interestRate}%/năm</span>
                  </div>
                </div>
                <div className="flex items-center justify-between text-[11px] font-mono text-slate-400 pt-1 border-t border-slate-800/80">
                  <span>Thời hạn: {l.termMonths} tháng</span>
                  <span>Phương thức: Gốc đều + Lãi giảm dần</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
