import React, { useState, useEffect } from 'react';
import { AccountDto, TransferResponse } from '../../core/types/banking';
import { paymentsApi } from '../../core/api/httpClient';
import { generateUUID, formatCurrency } from '../../core/utils/formatters';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { Send, RefreshCw, AlertCircle, Sparkles } from 'lucide-react';
import confetti from 'canvas-confetti';

interface TransferTerminalProps {
  accounts: AccountDto[];
  onTransferComplete: (res: TransferResponse) => void;
  onSagaStepChange?: (step: any, status: any) => void;
}

export const TransferTerminal: React.FC<TransferTerminalProps> = ({
  accounts,
  onTransferComplete,
  onSagaStepChange,
}) => {
  const [sourceId, setSourceId] = useState(accounts[0]?.id || '');
  const [destId, setDestId] = useState(accounts[1]?.id || '');
  const [amount, setAmount] = useState('25000');
  const [idempotencyKey, setIdempotencyKey] = useState(`IDEM-${generateUUID().substring(0, 8)}`);
  const [description] = useState('Interbank settlement payment');
  const [simulateFraud, setSimulateFraud] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (accounts.length >= 2) {
      if (!sourceId) setSourceId(accounts[0].id);
      if (!destId) setDestId(accounts[1].id);
    }
  }, [accounts]);

  const sourceAccount = accounts.find((a) => a.id === sourceId);

  const handleExecute = async (e: React.FormEvent) => {
    e.preventDefault();
    if (sourceId === destId) {
      setErrorMessage('Source and Destination accounts must be different');
      return;
    }
    const transferAmount = parseFloat(amount);
    if (isNaN(transferAmount) || transferAmount <= 0) {
      setErrorMessage('Transfer amount must be strictly greater than 0');
      return;
    }
    if (sourceAccount && sourceAccount.availableBalance < transferAmount) {
      setErrorMessage('Insufficient available balance on source account');
      return;
    }

    setIsLoading(true);
    setErrorMessage(null);

    // Animate saga step transitions for realistic visualization
    onSagaStepChange?.('STARTED', 'EXECUTING');
    await new Promise((r) => setTimeout(r, 400));
    onSagaStepChange?.('FUNDS_HELD', 'EXECUTING');
    await new Promise((r) => setTimeout(r, 450));
    onSagaStepChange?.('FRAUD_EVALUATED', 'EXECUTING');
    await new Promise((r) => setTimeout(r, 400));

    try {
      const finalDesc = simulateFraud
        ? `${description} TRIGGER_FRAUD`
        : description;

      const res = await paymentsApi.transfer({
        sourceAccountId: sourceId,
        destinationAccountId: destId,
        amount: transferAmount,
        currency: sourceAccount?.currency || 'USD',
        idempotencyKey,
        description: finalDesc,
      });

      if (res.data.status === 'COMPENSATED') {
        onSagaStepChange?.('COMPENSATED', 'COMPENSATED');
      } else {
        onSagaStepChange?.('COMMITTED', 'COMPLETED');
        confetti({
          particleCount: 80,
          spread: 70,
          origin: { y: 0.6 },
          colors: ['#6366f1', '#10b981', '#38bdf8'],
        });
      }

      onTransferComplete(res.data);
      setIdempotencyKey(`IDEM-${generateUUID().substring(0, 8)}`);
    } catch (err: any) {
      setErrorMessage(err.message || 'Payment transfer failed');
      onSagaStepChange?.('COMPENSATED', 'FAILED');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Card
      header={
        <div className="flex items-center justify-between w-full">
          <div className="flex items-center gap-2">
            <Send className="w-4 h-4 text-emerald-400" />
            <h4 className="text-sm font-semibold text-slate-100">
              Payment Orchestration Terminal
            </h4>
          </div>
          <span className="text-xs font-mono text-emerald-400 font-semibold bg-emerald-500/10 px-2.5 py-0.5 rounded-full border border-emerald-500/20">
            Pessimistic Lock Active
          </span>
        </div>
      }
    >
      <form onSubmit={handleExecute} className="space-y-4">
        {errorMessage && (
          <div className="p-3 bg-rose-500/10 border border-rose-500/30 rounded-lg text-rose-400 text-xs font-medium flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{errorMessage}</span>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Source Account */}
          <div className="space-y-1.5">
            <label className="block text-xs font-semibold text-slate-300">
              Source Account (Debit)
            </label>
            <select
              value={sourceId}
              onChange={(e) => setSourceId(e.target.value)}
              className="w-full bg-slate-900 border border-slate-700/80 rounded-lg px-3 py-2 text-sm text-slate-200 outline-none focus:border-indigo-500"
            >
              {accounts.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.accountNumber} — {a.holderName} (
                  {formatCurrency(a.availableBalance, a.currency)})
                </option>
              ))}
            </select>
          </div>

          {/* Destination Account */}
          <div className="space-y-1.5">
            <label className="block text-xs font-semibold text-slate-300">
              Destination Account (Credit)
            </label>
            <select
              value={destId}
              onChange={(e) => setDestId(e.target.value)}
              className="w-full bg-slate-900 border border-slate-700/80 rounded-lg px-3 py-2 text-sm text-slate-200 outline-none focus:border-indigo-500"
            >
              {accounts.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.accountNumber} — {a.holderName}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Amount & Quick Amounts */}
        <div className="space-y-2">
          <Input
            label="Transfer Amount"
            type="number"
            min="1"
            step="any"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            rightAddon={
              <span className="font-mono text-xs text-slate-400">
                {sourceAccount?.currency || 'USD'}
              </span>
            }
            required
          />

          <div className="flex items-center gap-2 pt-1">
            <span className="text-[11px] text-slate-400">Presets:</span>
            {[1000, 25000, 75000, 150000].map((preset) => (
              <button
                key={preset}
                type="button"
                onClick={() => setAmount(String(preset))}
                className="px-2 py-0.5 rounded bg-slate-800 hover:bg-slate-700 border border-slate-700 text-xs font-mono text-slate-300"
              >
                ${preset.toLocaleString()}
              </button>
            ))}
          </div>
        </div>

        {/* Idempotency Key with refresh button */}
        <div className="space-y-1.5">
          <Input
            label="Idempotency Key (RFC Header)"
            value={idempotencyKey}
            onChange={(e) => setIdempotencyKey(e.target.value)}
            rightAddon={
              <button
                type="button"
                onClick={() => setIdempotencyKey(`IDEM-${generateUUID().substring(0, 8)}`)}
                className="text-indigo-400 hover:text-indigo-300 p-1"
                title="Generate new idempotency key"
              >
                <RefreshCw className="w-3.5 h-3.5" />
              </button>
            }
            helperText="Prevents duplicate charges when client retries over unstable network."
            required
          />
        </div>

        {/* Simulation flag */}
        <div className="p-3 bg-slate-900/60 rounded-lg border border-slate-800 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-amber-400" />
            <span className="text-xs text-slate-300 font-medium">
              Simulate Fraud / Partner Network Failure
            </span>
          </div>
          <input
            type="checkbox"
            checked={simulateFraud}
            onChange={(e) => setSimulateFraud(e.target.checked)}
            className="w-4 h-4 accent-indigo-500 rounded cursor-pointer"
          />
        </div>

        <Button
          type="submit"
          variant="primary"
          size="lg"
          isLoading={isLoading}
          icon={<Send className="w-4 h-4" />}
          className="w-full"
        >
          Execute Distributed Transfer (Saga)
        </Button>
      </form>
    </Card>
  );
};
