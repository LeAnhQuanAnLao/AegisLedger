import React from 'react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
  leftAddon?: React.ReactNode;
  rightAddon?: React.ReactNode;
}

export const Input: React.FC<InputProps> = ({
  label,
  error,
  helperText,
  leftAddon,
  rightAddon,
  className = '',
  id,
  ...props
}) => {
  const inputId = id || props.name;

  return (
    <div className="w-full space-y-1.5">
      {label && (
        <label htmlFor={inputId} className="block text-xs font-semibold text-slate-300 tracking-wide">
          {label}
        </label>
      )}
      <div className="relative flex items-center rounded-lg bg-slate-900/90 border border-slate-700/80 focus-within:border-indigo-500 focus-within:ring-1 focus-within:ring-indigo-500 transition-all">
        {leftAddon && (
          <div className="pl-3 pr-2 text-slate-400 select-none flex items-center">{leftAddon}</div>
        )}
        <input
          id={inputId}
          className={`w-full bg-transparent px-3 py-2 text-sm text-slate-100 placeholder-slate-500 outline-none disabled:opacity-50 disabled:cursor-not-allowed ${className}`}
          {...props}
        />
        {rightAddon && (
          <div className="pr-3 pl-2 text-slate-400 select-none flex items-center">{rightAddon}</div>
        )}
      </div>
      {error && <p className="text-xs text-rose-400 font-medium">{error}</p>}
      {helperText && !error && <p className="text-xs text-slate-500">{helperText}</p>}
    </div>
  );
};
