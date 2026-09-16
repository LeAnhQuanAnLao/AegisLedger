import React from 'react';
import {
  LayoutDashboard,
  CreditCard,
  Send,
  BookOpen,
  ShieldAlert,
  Server,
  Layers,
  BarChart3,
  UserCheck,
} from 'lucide-react';

export type NavTab =
  | 'admin-portal'
  | 'user-portal'
  | 'overview'
  | 'accounts'
  | 'transfer'
  | 'ledger'
  | 'fraud'
  | 'telemetry';

interface SidebarProps {
  activeTab: NavTab;
  onTabChange: (tab: NavTab) => void;
  isMockMode: boolean;
}

export const Sidebar: React.FC<SidebarProps> = ({
  activeTab,
  onTabChange,
  isMockMode,
}) => {
  const navItems: { id: NavTab; label: string; icon: any; badge?: string }[] = [
    { id: 'admin-portal', label: 'Admin & Tần Suất GD', icon: BarChart3, badge: 'ADMIN' },
    { id: 'user-portal', label: 'Khách Hàng & Sao Kê', icon: UserCheck, badge: 'USER' },
    { id: 'overview', label: 'Dashboard Tổng Quan', icon: LayoutDashboard },
    { id: 'accounts', label: 'Quản Lý Tài Khoản', icon: CreditCard },
    { id: 'transfer', label: 'Saga Payment Studio', icon: Send },
    { id: 'ledger', label: 'Sổ Cái Kép (Ledger)', icon: BookOpen },
    { id: 'fraud', label: 'Chống Gian Lận (Fraud)', icon: ShieldAlert },
    { id: 'telemetry', label: 'Hạ Tầng & Telemetry', icon: Server },
  ];

  return (
    <aside className="w-64 bg-[#0B0F19] border-r border-slate-800/80 flex flex-col justify-between p-4 shrink-0 select-none">
      <div className="space-y-6">
        {/* Brand Header */}
        <div className="flex items-center gap-3 px-2 py-1">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-indigo-500 to-indigo-700 flex items-center justify-center text-white shadow-glow-indigo">
            <Layers className="w-5 h-5 stroke-[2.5]" />
          </div>
          <div>
            <h1 className="text-base font-bold tracking-tight text-white flex items-center gap-1.5">
              AegisLedger
              <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-indigo-500/20 text-indigo-400 border border-indigo-500/30">
                CORE
              </span>
            </h1>
            <p className="text-[10px] text-slate-400 font-mono">Fintech Orchestration</p>
          </div>
        </div>

        {/* Navigation List */}
        <nav className="space-y-1">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeTab === item.id;

            return (
              <button
                key={item.id}
                onClick={() => onTabChange(item.id)}
                className={`w-full flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-semibold transition-all duration-150 ${
                  isActive
                    ? 'bg-indigo-600 text-white shadow-glow-indigo'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
                }`}
              >
                <div className="flex items-center gap-3">
                  <Icon className={`w-4 h-4 ${isActive ? 'text-white' : 'text-slate-400'}`} />
                  <span>{item.label}</span>
                </div>
                {item.badge && (
                  <span
                    className={`text-[9px] font-mono px-1.5 py-0.5 rounded font-bold ${
                      isActive
                        ? 'bg-white/20 text-white'
                        : item.badge === 'ADMIN'
                        ? 'bg-indigo-500/20 text-indigo-400 border border-indigo-500/30'
                        : 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                    }`}
                  >
                    {item.badge}
                  </span>
                )}
              </button>
            );
          })}
        </nav>
      </div>

      {/* Footer Environment Tag */}
      <div className="p-3 rounded-xl bg-slate-900/60 border border-slate-800/80 space-y-1 text-xs">
        <div className="flex items-center justify-between">
          <span className="text-[10px] text-slate-500 font-mono uppercase">Environment</span>
          <span
            className={`w-2 h-2 rounded-full ${
              isMockMode ? 'bg-amber-400' : 'bg-emerald-400 animate-pulse'
            }`}
          />
        </div>
        <p className="font-mono text-xs font-semibold text-slate-200">
          {isMockMode ? 'Standalone Demo' : 'Live Spring API'}
        </p>
        <p className="text-[10px] text-slate-500">Virtual Threads: Active</p>
      </div>
    </aside>
  );
};
