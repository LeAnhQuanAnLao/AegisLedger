import React, { useState } from 'react';
import { Currency } from '../../core/types/banking';
import { accountsApi } from '../../core/api/httpClient';
import { Modal } from '../../components/ui/Modal';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { PlusCircle } from 'lucide-react';

interface CreateAccountModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const CreateAccountModal: React.FC<CreateAccountModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [accountNumber, setAccountNumber] = useState(
    `ACC-${Math.floor(10000000 + Math.random() * 90000000)}`
  );
  const [holderName, setHolderName] = useState('');
  const [currency, setCurrency] = useState<Currency>('USD');
  const [initialDeposit, setInitialDeposit] = useState('100000');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!holderName.trim()) {
      setError('Account holder name is required');
      return;
    }
    const depositNum = parseFloat(initialDeposit);
    if (isNaN(depositNum) || depositNum < 0) {
      setError('Initial deposit must be a non-negative number');
      return;
    }

    setIsLoading(true);
    setError(null);
    try {
      await accountsApi.create({
        accountNumber,
        holderName: holderName.trim(),
        currency,
        initialDeposit: depositNum,
      });
      onSuccess();
      onClose();
      // Reset form
      setAccountNumber(`ACC-${Math.floor(10000000 + Math.random() * 90000000)}`);
      setHolderName('');
      setInitialDeposit('100000');
    } catch (err: any) {
      setError(err.message || 'Failed to create account');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Open New Bank Account">
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 bg-rose-500/10 border border-rose-500/30 rounded-lg text-rose-400 text-xs font-medium">
            {error}
          </div>
        )}

        <Input
          label="Account Number (Generated or Custom)"
          value={accountNumber}
          onChange={(e) => setAccountNumber(e.target.value)}
          required
        />

        <Input
          label="Legal Entity / Holder Name"
          placeholder="e.g. Acme Corp Treasury"
          value={holderName}
          onChange={(e) => setHolderName(e.target.value)}
          required
        />

        <div className="space-y-1.5">
          <label className="block text-xs font-semibold text-slate-300">Currency</label>
          <div className="grid grid-cols-4 gap-2">
            {(['USD', 'EUR', 'VND', 'SGD'] as Currency[]).map((c) => (
              <button
                key={c}
                type="button"
                onClick={() => setCurrency(c)}
                className={`py-2 text-xs font-mono font-bold rounded-lg border transition-all ${
                  currency === c
                    ? 'bg-indigo-600/30 border-indigo-500 text-indigo-300 shadow-glow-indigo'
                    : 'bg-slate-900 border-slate-700 text-slate-400 hover:text-slate-200'
                }`}
              >
                {c}
              </button>
            ))}
          </div>
        </div>

        <Input
          label="Initial Deposit Amount"
          type="number"
          min="0"
          step="any"
          value={initialDeposit}
          onChange={(e) => setInitialDeposit(e.target.value)}
          rightAddon={<span className="font-mono text-xs">{currency}</span>}
          required
        />

        <div className="pt-3 flex items-center justify-end gap-3 border-t border-slate-800">
          <Button type="button" variant="ghost" onClick={onClose} disabled={isLoading}>
            Cancel
          </Button>
          <Button type="submit" variant="primary" isLoading={isLoading} icon={<PlusCircle className="w-4 h-4" />}>
            Create Account
          </Button>
        </div>
      </form>
    </Modal>
  );
};
