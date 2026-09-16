import React from 'react';
import {
  AccountDto,
  TransferResponse,
  SagaStep,
  TransactionStatus,
  Transaction,
} from '../core/types/banking';
import { TransferTerminal } from '../modules/transfer/TransferTerminal';
import { SagaVisualizer } from '../modules/saga/SagaVisualizer';
import { Table } from '../components/ui/Table';
import { Badge } from '../components/ui/Badge';
import { formatCurrency, formatDateTime, truncateId } from '../core/utils/formatters';

interface TransferStudioProps {
  accounts: AccountDto[];
  transactions: Transaction[];
  activeStep: SagaStep;
  activeStatus: TransactionStatus;
  activeTxId?: string;
  activeIdemKey?: string;
  onTransferComplete: (res: TransferResponse) => void;
  onSagaStepChange: (step: SagaStep, status: TransactionStatus) => void;
}

export const TransferStudio: React.FC<TransferStudioProps> = ({
  accounts,
  transactions,
  activeStep,
  activeStatus,
  activeTxId,
  activeIdemKey,
  onTransferComplete,
  onSagaStepChange,
}) => {
  const txColumns = [
    {
      header: 'Timestamp',
      className: 'w-44',
      render: (tx: Transaction) => (
        <span className="font-mono text-xs text-slate-400">
          {formatDateTime(tx.createdAt)}
        </span>
      ),
    },
    {
      header: 'Tx UUID',
      className: 'w-36 font-mono',
      render: (tx: Transaction) => (
        <span className="text-xs text-slate-300 font-mono">
          {truncateId(tx.id, 6, 4)}
        </span>
      ),
    },
    {
      header: 'Idempotency Key',
      className: 'w-40 font-mono text-xs text-slate-400',
      accessor: 'idempotencyKey' as const,
    },
    {
      header: 'Amount',
      className: 'w-36 font-mono text-right',
      render: (tx: Transaction) => (
        <span className="font-bold text-slate-100 font-mono">
          {formatCurrency(tx.amount, tx.currency)}
        </span>
      ),
    },
    {
      header: 'Status',
      className: 'w-32',
      render: (tx: Transaction) => (
        <Badge
          variant={
            tx.status === 'COMPLETED'
              ? 'success'
              : tx.status === 'COMPENSATED'
              ? 'warning'
              : 'danger'
          }
        >
          {tx.status}
        </Badge>
      ),
    },
    {
      header: 'Failure / Note',
      render: (tx: Transaction) => (
        <span className="text-xs text-slate-400 truncate max-w-xs block">
          {tx.failureReason || 'Normal execution'}
        </span>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h3 className="text-lg font-bold text-slate-100 tracking-tight">
          Payment Saga Studio & Orchestrator
        </h3>
        <p className="text-xs text-slate-400 mt-0.5">
          Execute multi-step distributed transfers with Idempotency locks and real-time Saga Compensator rollback.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <div className="lg:col-span-5">
          <TransferTerminal
            accounts={accounts}
            onTransferComplete={onTransferComplete}
            onSagaStepChange={onSagaStepChange}
          />
        </div>

        <div className="lg:col-span-7 space-y-6">
          <SagaVisualizer
            currentStep={activeStep}
            status={activeStatus}
            transactionId={activeTxId || transactions[0]?.id}
            idempotencyKey={activeIdemKey || transactions[0]?.idempotencyKey}
            isRunning={activeStatus === 'EXECUTING'}
          />

          <div className="space-y-3">
            <h4 className="text-sm font-semibold text-slate-100">
              Orchestrated Transaction History
            </h4>
            <Table
              columns={txColumns}
              data={transactions}
              keyExtractor={(tx) => tx.id}
              emptyMessage="No orchestrated transactions found."
            />
          </div>
        </div>
      </div>
    </div>
  );
};
