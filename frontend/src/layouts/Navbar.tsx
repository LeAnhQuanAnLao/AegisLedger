import React from 'react';
import { Button } from '../components/ui/Button';
import { Send, Zap, Globe, RefreshCw } from 'lucide-react';

interface NavbarProps {
  activeTabTitle: string;
  isMockMode: boolean;
  onToggleMockMode: () => void;
  onQuickTransfer: () => void;
  onRefreshData: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  activeTabTitle,
  isMockMode,
  onToggleMockMode,
  onQuickTransfer,
  onRefreshData,
}) => {
  return (
    <header className="h-16 px-6 border-b border-slate-800/80 bg-[#0B0F19]/80 backdrop-blur-md flex items-center justify-between z-10 sticky top-0">
      <div className="flex items-center gap-3">
        <h2 className="text-base font-semibold text-slate-100 tracking-tight">
          {activeTabTitle}
        </h2>
        <span className="text-xs font-mono text-slate-500 hidden sm:inline">/</span>
        <span className="text-xs text-slate-400 hidden sm:inline">
          High-Throughput Orchestrator Console
        </span>
      </div>

      <div className="flex items-center gap-3">
        {/* Mode Switcher */}
        <button
          onClick={onToggleMockMode}
          className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold border transition-all cursor-pointer ${
            isMockMode
              ? 'bg-amber-500/10 border-amber-500/30 text-amber-300 hover:bg-amber-500/20'
              : 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300 hover:bg-emerald-500/20'
          }`}
          title="Click to toggle between Standalone Demo Mode and Live Spring Boot API"
        >
          <Globe className="w-3.5 h-3.5" />
          <span>{isMockMode ? 'Demo Mode' : 'Live API (8080)'}</span>
        </button>

        {/* Latency Indicator */}
        <div className="hidden md:flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-slate-900 border border-slate-800 text-xs font-mono text-slate-300">
          <Zap className="w-3 h-3 text-emerald-400" />
          <span>{isMockMode ? '0.4ms' : '18ms'}</span>
        </div>

        {/* Refresh Button */}
        <button
          onClick={onRefreshData}
          className="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 border border-transparent hover:border-slate-700 transition-colors"
          title="Refresh All Data"
        >
          <RefreshCw className="w-4 h-4" />
        </button>

        {/* Quick Transfer CTA */}
        <Button
          variant="primary"
          size="sm"
          icon={<Send className="w-3.5 h-3.5" />}
          onClick={onQuickTransfer}
        >
          Transfer Funds
        </Button>
      </div>
    </header>
  );
};
