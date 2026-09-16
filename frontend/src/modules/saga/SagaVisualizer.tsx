import React from 'react';
import { SagaStep, TransactionStatus } from '../../core/types/banking';
import { SAGA_STEPS, STATUS_COLORS } from '../../core/utils/sagaConstants';
import { Stepper, StepItem, StepState } from '../../components/ui/Stepper';
import { Badge } from '../../components/ui/Badge';
import { Card } from '../../components/ui/Card';
import { AlertTriangle, ShieldCheck, Zap } from 'lucide-react';

interface SagaVisualizerProps {
  currentStep: SagaStep;
  status: TransactionStatus;
  transactionId?: string;
  idempotencyKey?: string;
  failureReason?: string | null;
  isRunning?: boolean;
}

export const SagaVisualizer: React.FC<SagaVisualizerProps> = ({
  currentStep,
  status,
  transactionId,
  idempotencyKey,
  failureReason,
  isRunning = false,
}) => {
  const stepOrder: SagaStep[] = [
    'STARTED',
    'FUNDS_HELD',
    'FRAUD_EVALUATED',
    'SWITCH_PROCESSED',
    'COMMITTED',
  ];

  const currentIdx = stepOrder.indexOf(currentStep);

  const steps: StepItem[] = SAGA_STEPS.map((s, idx) => {
    let state: StepState = 'pending';

    if (status === 'COMPENSATED') {
      if (idx < currentIdx) state = 'completed';
      else if (idx === currentIdx) state = 'compensated';
      else state = 'pending';
    } else if (status === 'FAILED') {
      if (idx < currentIdx) state = 'completed';
      else if (idx === currentIdx) state = 'failed';
      else state = 'pending';
    } else if (status === 'COMPLETED') {
      state = 'completed';
    } else if (isRunning) {
      if (idx < currentIdx) state = 'completed';
      else if (idx === currentIdx) state = 'running';
      else state = 'pending';
    } else {
      if (idx <= currentIdx) state = 'completed';
    }

    return {
      id: s.step,
      label: s.label,
      subTitle: s.subTitle,
      state,
    };
  });

  const activeColor = STATUS_COLORS[status] || STATUS_COLORS.PENDING;

  return (
    <Card
      header={
        <div className="flex items-center justify-between w-full">
          <div className="flex items-center gap-2">
            <Zap className="w-5 h-5 text-indigo-400" />
            <h4 className="text-sm font-semibold text-slate-100">
              Distributed Saga Orchestration Pipeline
            </h4>
          </div>
          <Badge
            variant={
              status === 'COMPLETED'
                ? 'success'
                : status === 'COMPENSATED'
                ? 'warning'
                : status === 'FAILED'
                ? 'danger'
                : 'info'
            }
            pulse={isRunning}
          >
            {status}
          </Badge>
        </div>
      }
      className="space-y-4"
    >
      <Stepper steps={steps} className="my-2" />

      {/* Status details bar */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-3 border-t border-slate-800/80 text-xs font-mono">
        <div className="bg-slate-900/60 p-2.5 rounded-lg border border-slate-800">
          <p className="text-slate-500 uppercase text-[10px]">Idempotency Key</p>
          <p className="text-slate-300 truncate font-semibold">
            {idempotencyKey || 'Waiting for submission...'}
          </p>
        </div>
        <div className="bg-slate-900/60 p-2.5 rounded-lg border border-slate-800">
          <p className="text-slate-500 uppercase text-[10px]">Active Saga Step</p>
          <p className={`font-semibold ${activeColor.text}`}>{currentStep}</p>
        </div>
        <div className="bg-slate-900/60 p-2.5 rounded-lg border border-slate-800">
          <p className="text-slate-500 uppercase text-[10px]">Tx UUID</p>
          <p className="text-slate-300 truncate font-semibold">
            {transactionId || '--'}
          </p>
        </div>
      </div>

      {/* Compensation / Failure Alert */}
      {status === 'COMPENSATED' && (
        <div className="p-3.5 rounded-xl bg-amber-500/10 border border-amber-500/30 flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
          <div className="space-y-1 text-xs">
            <p className="font-semibold text-amber-300">
              Saga Automatic Compensation Triggered
            </p>
            <p className="text-slate-300 leading-relaxed">
              {failureReason ||
                'Partner gateway timeout or fraud threshold violation detected. All held funds were automatically unlocked and reversed to the source account.'}
            </p>
          </div>
        </div>
      )}

      {status === 'COMPLETED' && (
        <div className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center gap-3">
          <ShieldCheck className="w-5 h-5 text-emerald-400 shrink-0" />
          <p className="text-xs text-emerald-300 font-medium">
            Atomic Saga Completed: Both ledger entries (DEBIT & CREDIT) have been immutably committed and Outbox event published.
          </p>
        </div>
      )}
    </Card>
  );
};
