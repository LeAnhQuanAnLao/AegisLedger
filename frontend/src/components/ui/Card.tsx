import React from 'react';

interface CardProps {
  children: React.ReactNode;
  className?: string;
  header?: React.ReactNode;
  footer?: React.ReactNode;
  glow?: 'indigo' | 'emerald' | 'amber' | 'rose' | 'none';
}

const GLOW_MAP = {
  indigo: 'hover:border-indigo-500/50 hover:shadow-glow-indigo',
  emerald: 'hover:border-emerald-500/50 hover:shadow-glow-emerald',
  amber: 'hover:border-amber-500/50 hover:shadow-glow-amber',
  rose: 'hover:border-rose-500/50 hover:shadow-glow-rose',
  none: '',
};

export const Card: React.FC<CardProps> = ({
  children,
  className = '',
  header,
  footer,
  glow = 'none',
}) => {
  return (
    <div
      className={`glass-panel rounded-xl overflow-hidden transition-all duration-200 ${GLOW_MAP[glow]} ${className}`}
    >
      {header && (
        <div className="px-5 py-4 border-b border-slate-800/80 bg-slate-900/30 flex items-center justify-between">
          {header}
        </div>
      )}
      <div className="p-5">{children}</div>
      {footer && (
        <div className="px-5 py-3 border-t border-slate-800/80 bg-slate-900/40 text-xs text-slate-400">
          {footer}
        </div>
      )}
    </div>
  );
};
