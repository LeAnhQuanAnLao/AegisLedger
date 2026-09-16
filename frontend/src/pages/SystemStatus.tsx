import React from 'react';
import { SystemHealthCard } from '../modules/telemetry/SystemHealthCard';
import { Card } from '../components/ui/Card';
import { Network } from 'lucide-react';

export const SystemStatus: React.FC = () => {
  return (
    <div className="space-y-6">
      <div>
        <h3 className="text-lg font-bold text-slate-100 tracking-tight">
          System Runtime & Event-Driven Architecture
        </h3>
        <p className="text-xs text-slate-400 mt-0.5">
          Real-time health monitoring of Java 21 Virtual Threads (Loom), PostgreSQL 16, Redis 7, and Kafka.
        </p>
      </div>

      <SystemHealthCard />

      <Card
        header={
          <div className="flex items-center gap-2">
            <Network className="w-4 h-4 text-indigo-400" />
            <h4 className="text-sm font-semibold text-slate-100">
              Event-Driven Micro-Architecture Topology
            </h4>
          </div>
        }
      >
        <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 font-mono text-xs text-slate-300 overflow-x-auto">
          <pre className="leading-relaxed">
{`┌───────────────────────────┐      REST / Idempotency-Key
│   AegisLedger React App   │ ─────────────────────────────────► ┌───────────────────────────┐
└───────────────────────────┘                                    │    Payment Controller     │
                                                                 └─────────────┬─────────────┘
                                                                               │
                                       ┌───────────────────────────────────────┴───────────────────────────────────────┐
                                       ▼                                                                               ▼
                        ┌─────────────────────────────┐                                                 ┌─────────────────────────────┐
                        │      Saga Coordinator       │                                                 │    Idempotency Service      │
                        │    (Multi-step Workflow)    │                                                 │     (Redis Distributed)     │
                        └──────────────┬──────────────┘                                                 └─────────────────────────────┘
                                       │
        ┌──────────────────────────────┼──────────────────────────────┬──────────────────────────────┐
        ▼                              ▼                              ▼                              ▼
┌──────────────┐               ┌──────────────┐               ┌──────────────┐               ┌──────────────┐
│Account Domain│               │ Fraud Engine │               │ Ledger Core  │               │Outbox Worker │
│Pessimistic DB│               │Redis Sliding │               │Double-Entry  │               │Kafka Event   │
│SELECT FOR UPD│               │Window Rules  │               │Immutable DB  │               │Virtual Thread│
└──────────────┘               └──────────────┘               └──────────────┘               └──────────────┘`}
          </pre>
        </div>
      </Card>
    </div>
  );
};
