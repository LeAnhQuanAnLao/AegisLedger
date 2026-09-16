import React from 'react';
import { LucideIcon } from 'lucide-react';

interface StatCardProps {
  title: string;
  value: string | number;
  change?: string;
  isPositive?: boolean;
  icon: LucideIcon;
  subtitle?: string;
  badgeColor?: 'emerald' | 'indigo' | 'amber' | 'cyan';
}

const COLOR_MAP = {
  emerald: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
  indigo: 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20',
  amber: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
  cyan: 'bg-cyan-500/10 text-cyan-400 border-cyan-500/20',
};

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  change,
  isPositive = true,
  icon: Icon,
  subtitle,
  badgeColor = 'indigo',
}) => {
  return (
    <div className="glass-panel p-5 rounded-xl flex items-start justify-between border border-slate-800/80 hover:border-slate-700 transition-colors">
      <div className="space-y-1">
        <p className="text-xs font-medium uppercase tracking-wider text-slate-400">{title}</p>
        <p className="text-2xl font-bold font-mono text-white tracking-tight">{value}</p>
        {(change || subtitle) && (
          <div className="flex items-center gap-2 pt-1 text-xs">
            {change && (
              <span
                className={`font-semibold font-mono ${
                  isPositive ? 'text-emerald-400' : 'text-rose-400'
                }`}
              >
                {change}
              </span>
            )}
            {subtitle && <span className="text-slate-500">{subtitle}</span>}
          </div>
        )}
      </div>
      <div className={`p-3 rounded-lg border ${COLOR_MAP[badgeColor]}`}>
        <Icon className="w-5 h-5" />
      </div>
    </div>
  );
};
