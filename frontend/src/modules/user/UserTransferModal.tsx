import React, { useState, useEffect } from 'react';
import { Send, AlertCircle, Info } from 'lucide-react';
import { AccountDto, TransferResponse } from '../../core/types/banking';
import { paymentsApi } from '../../core/api/httpClient';
import { feeLimitApi } from '../../core/api/bankingServicesApi';
import { formatCurrency } from '../../core/utils/formatters';
import { Button } from '../../components/ui/Button';

interface UserTransferModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentAccount: AccountDto;
  accounts: AccountDto[];
  onSuccess: (res: TransferResponse) => void;
}

export const UserTransferModal: React.FC<UserTransferModalProps> = ({
  isOpen,
  onClose,
  currentAccount,
  accounts,
  onSuccess,
}) => {
  const [destAccountId, setDestAccountId] = useState('');
  const [amountStr, setAmountStr] = useState('');
  const [description, setDescription] = useState('');
  const [fee, setFee] = useState<number>(0.5);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const destinationCandidates = accounts.filter((a) => a.id !== currentAccount?.id);

  useEffect(() => {
    if (destinationCandidates.length > 0 && !destAccountId) {
      setDestAccountId(destinationCandidates[0].id);
    }
  }, [destinationCandidates, destAccountId]);

  const numAmount = parseFloat(amountStr) || 0;
  const totalDeduction = numAmount + fee;

  // Fee calculation preview
  useEffect(() => {
    if (numAmount > 0) {
      feeLimitApi.previewFee(numAmount, currentAccount?.currency || 'USD')
        .then((res) => {
          if (res.data?.feeAmount) {
            setFee(res.data.feeAmount.amount);
          }
        })
        .catch(() => {
          setFee(Math.max(0.5, numAmount * 0.001));
        });
    } else {
      setFee(0.5);
    }
  }, [numAmount, currentAccount]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!destAccountId || numAmount <= 0) {
      setError('Vui lòng nhập số tiền hợp lệ và chọn tài khoản nhận.');
      return;
    }
    if (numAmount > currentAccount.availableBalance) {
      setError('Số dư khả dụng không đủ để thực hiện giao dịch.');
      return;
    }

    setError(null);
    setLoading(true);
    try {
      const idemKey = `IDEM-USER-${Date.now()}-${Math.floor(Math.random() * 1000)}`;
      const res = await paymentsApi.transfer({
        sourceAccountId: currentAccount.id,
        destinationAccountId: destAccountId,
        amount: numAmount,
        currency: currentAccount.currency,
        idempotencyKey: idemKey,
        description: description || `Transfer to ${destAccountId}`,
      });

      if (res.data) {
        onSuccess(res.data);
        onClose();
      }
    } catch (err: any) {
      setError(err.message || 'Chuyển tiền thất bại');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fade-in">
      <div className="w-full max-w-md bg-[#0F1422] border border-slate-700/80 rounded-2xl shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-800 flex items-center justify-between bg-slate-900/50">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-lg bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
              <Send className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-white">Chuyển Tiền Nhanh (Saga Payment)</h3>
              <p className="text-xs text-slate-400">Từ: {currentAccount?.holderName}</p>
            </div>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-white text-sm font-mono">✕</button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/30 flex items-center gap-2 text-xs text-rose-400">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* Destination Account */}
          <div className="space-y-1.5">
            <label className="text-xs font-mono text-slate-300">Tài khoản thụ hưởng</label>
            <select
              value={destAccountId}
              onChange={(e) => setDestAccountId(e.target.value)}
              className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 font-mono focus:outline-none focus:border-indigo-500"
            >
              {destinationCandidates.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.holderName} - {a.accountNumber}
                </option>
              ))}
            </select>
          </div>

          {/* Amount */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between text-xs font-mono">
              <label className="text-slate-300">Số tiền ({currentAccount?.currency})</label>
              <span className="text-slate-400">
                Khả dụng: {formatCurrency(currentAccount?.availableBalance || 0, currentAccount?.currency || 'USD')}
              </span>
            </div>
            <input
              type="number"
              min="1"
              step="any"
              value={amountStr}
              onChange={(e) => setAmountStr(e.target.value)}
              placeholder="VD: 500"
              className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-indigo-500"
            />
          </div>

          {/* Description */}
          <div className="space-y-1.5">
            <label className="text-xs font-mono text-slate-300">Nội dung chuyển tiền (Tùy chọn)</label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Thanh toán hóa đơn / Chuyển khoản"
              className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-indigo-500"
            />
          </div>

          {/* Real-time Fee Preview Breakdown */}
          <div className="p-3 rounded-xl bg-slate-900/70 border border-slate-800 space-y-1.5 text-xs font-mono">
            <div className="flex justify-between text-slate-400">
              <span>Số tiền gửi:</span>
              <span className="text-slate-200">{formatCurrency(numAmount, 'USD')}</span>
            </div>
            <div className="flex justify-between text-slate-400">
              <span className="flex items-center gap-1">
                Phí giao dịch (0.1% or min $0.50):
                <Info className="w-3 h-3 text-slate-500" />
              </span>
              <span className="text-indigo-400">+{formatCurrency(fee, 'USD')}</span>
            </div>
            <div className="flex justify-between text-white font-bold pt-1 border-t border-slate-800">
              <span>Tổng khấu trừ:</span>
              <span className="text-emerald-400">{formatCurrency(totalDeduction, 'USD')}</span>
            </div>
          </div>

          {/* Submit */}
          <div className="flex items-center justify-end gap-2 pt-2">
            <Button type="button" variant="ghost" size="sm" onClick={onClose} className="text-xs">
              Hủy
            </Button>
            <Button
              type="submit"
              disabled={loading || numAmount <= 0}
              size="sm"
              className="bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold"
            >
              {loading ? 'Đang điều phối Saga...' : 'Xác nhận chuyển tiền'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
