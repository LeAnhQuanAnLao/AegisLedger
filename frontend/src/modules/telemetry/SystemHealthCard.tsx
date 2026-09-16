import React from 'react';
import { Card } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Cpu, Database, Radio, Server, CheckCircle2 } from 'lucide-react';
import { MOCK_SYSTEM_METRICS } from '../../core/api/mockData';

export const SystemHealthCard: React.FC = () => {
  const metrics = MOCK_SYSTEM_METRICS;

  return (
    <Card
      header={
        <div className="flex items-center justify-between w-full">
          <div className="flex items-center gap-2">
            <Server className="w-4 h-4 text-indigo-400" />
            <h4 className="text-sm font-semibold text-slate-100">
              System Runtime & Architecture Health
            </h4>
          </div>
          <Badge variant="success" pulse>
            ALL SERVICES HEALTHY
          </Badge>
        </div>
      }
    >
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        {/* Java 21 Virtual Threads */}
        <div className="p-3 bg-slate-900/60 rounded-xl border border-slate-800 space-y-1">
          <div className="flex items-center justify-between">
            <span className="text-[10px] uppercase font-mono text-slate-500">Runtime</span>
            <Cpu className="w-3.5 h-3.5 text-indigo-400" />
          </div>
          <p className="text-sm font-semibold text-slate-100">Java 21 Virtual Threads</p>
          <p className="text-xs text-emerald-400 font-mono flex items-center gap-1">
            <CheckCircle2 className="w-3 h-3" />
            <span>Project Loom Active</span>
          </p>
        </div>

        {/* PostgreSQL Pool */}
        <div className="p-3 bg-slate-900/60 rounded-xl border border-slate-800 space-y-1">
          <div className="flex items-center justify-between">
            <span className="text-[10px] uppercase font-mono text-slate-500">PostgreSQL 16</span>
            <Database className="w-3.5 h-3.5 text-indigo-400" />
          </div>
          <p className="text-sm font-semibold text-slate-100">HikariCP Pool</p>
          <p className="text-xs text-slate-400 font-mono">
            {metrics.dbPoolActive} / {metrics.dbPoolMax} connections
          </p>
        </div>

        {/* Transactional Outbox Worker */}
        <div className="p-3 bg-slate-900/60 rounded-xl border border-slate-800 space-y-1">
          <div className="flex items-center justify-between">
            <span className="text-[10px] uppercase font-mono text-slate-500">Outbox Queue</span>
            <Radio className="w-3.5 h-3.5 text-indigo-400" />
          </div>
          <p className="text-sm font-semibold text-slate-100">Kafka Outbox Worker</p>
          <p className="text-xs text-emerald-400 font-mono">
            {metrics.outboxPendingCount} pending events
          </p>
        </div>

        {/* Avg P99 Latency */}
        <div className="p-3 bg-slate-900/60 rounded-xl border border-slate-800 space-y-1">
          <div className="flex items-center justify-between">
            <span className="text-[10px] uppercase font-mono text-slate-500">P99 Latency</span>
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
          </div>
          <p className="text-sm font-semibold text-slate-100 font-mono">{metrics.avgLatencyMs} ms</p>
          <p className="text-xs text-slate-500">Target &lt; 150ms</p>
        </div>
      </div>
    </Card>
  );
};
