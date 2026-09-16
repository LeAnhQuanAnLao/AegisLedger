import React from 'react';
import { Check, AlertTriangle, XCircle, Loader2 } from 'lucide-react';

export type StepState = 'pending' | 'running' | 'completed' | 'failed' | 'compensated';

export interface StepItem {
  id: string | number;
  label: string;
  subTitle?: string;
  state: StepState;
}

interface StepperProps {
  steps: StepItem[];
  className?: string;
}

export const Stepper: React.FC<StepperProps> = ({ steps, className = '' }) => {
  return (
    <div className={`w-full overflow-x-auto py-2 ${className}`}>
      <div className="flex items-center justify-between min-w-[640px]">
        {steps.map((step, idx) => {
          const isLast = idx === steps.length - 1;

          return (
            <React.Fragment key={step.id}>
              <div className="flex flex-col items-center text-center space-y-2 relative group">
                <div
                  className={`w-10 h-10 rounded-full flex items-center justify-center border-2 transition-all duration-300 ${
                    step.state === 'completed'
                      ? 'bg-emerald-500/20 border-emerald-500 text-emerald-400 shadow-glow-emerald'
                      : step.state === 'running'
                      ? 'bg-indigo-500/20 border-indigo-500 text-indigo-400 animate-pulse-slow shadow-glow-indigo'
                      : step.state === 'failed'
                      ? 'bg-rose-500/20 border-rose-500 text-rose-400 shadow-glow-rose'
                      : step.state === 'compensated'
                      ? 'bg-amber-500/20 border-amber-500 text-amber-400 shadow-glow-amber'
                      : 'bg-slate-900 border-slate-700 text-slate-500'
                  }`}
                >
                  {step.state === 'completed' && <Check className="w-5 h-5 stroke-[2.5]" />}
                  {step.state === 'running' && <Loader2 className="w-5 h-5 animate-spin" />}
                  {step.state === 'failed' && <XCircle className="w-5 h-5" />}
                  {step.state === 'compensated' && <AlertTriangle className="w-5 h-5" />}
                  {step.state === 'pending' && (
                    <span className="text-xs font-mono font-bold">{idx + 1}</span>
                  )}
                </div>

                <div className="space-y-0.5">
                  <p
                    className={`text-xs font-semibold whitespace-nowrap ${
                      step.state === 'completed'
                        ? 'text-emerald-300'
                        : step.state === 'running'
                        ? 'text-indigo-300'
                        : step.state === 'compensated'
                        ? 'text-amber-300'
                        : step.state === 'failed'
                        ? 'text-rose-300'
                        : 'text-slate-400'
                    }`}
                  >
                    {step.label}
                  </p>
                  {step.subTitle && (
                    <p className="text-[11px] text-slate-500 font-mono">{step.subTitle}</p>
                  )}
                </div>
              </div>

              {!isLast && (
                <div className="flex-1 h-0.5 mx-2 bg-slate-800 relative self-center mb-6">
                  <div
                    className={`h-full transition-all duration-500 ${
                      step.state === 'completed'
                        ? 'bg-emerald-500'
                        : step.state === 'compensated'
                        ? 'bg-amber-500'
                        : step.state === 'failed'
                        ? 'bg-rose-500'
                        : step.state === 'running'
                        ? 'bg-indigo-500 animate-pulse'
                        : 'bg-slate-800'
                    }`}
                  />
                </div>
              )}
            </React.Fragment>
          );
        })}
      </div>
    </div>
  );
};
