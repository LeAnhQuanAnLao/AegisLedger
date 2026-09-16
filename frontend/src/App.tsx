import { useState, useEffect, useCallback } from 'react';
import {
  AccountDto,
  LedgerEntryDto,
  Transaction,
  SagaStep,
  TransactionStatus,
  TransferResponse,
} from './core/types/banking';
import {
  accountsApi,
  paymentsApi,
  ledgerApi,
  getMockMode,
  setMockMode,
} from './core/api/httpClient';
import { Sidebar, NavTab } from './layouts/Sidebar';
import { Navbar } from './layouts/Navbar';
import { AdminPortal } from './pages/AdminPortal';
import { UserPortal } from './pages/UserPortal';
import { DashboardOverview } from './pages/DashboardOverview';
import { AccountsHub } from './pages/AccountsHub';
import { TransferStudio } from './pages/TransferStudio';
import { LedgerExplorer } from './pages/LedgerExplorer';
import { FraudConsole } from './pages/FraudConsole';
import { SystemStatus } from './pages/SystemStatus';
import { Modal } from './components/ui/Modal';
import { TransferTerminal } from './modules/transfer/TransferTerminal';

export function App() {
  const [activeTab, setActiveTab] = useState<NavTab>('admin-portal');
  const [isMock, setIsMock] = useState(getMockMode());
  const [accounts, setAccounts] = useState<AccountDto[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [ledgerEntries, setLedgerEntries] = useState<LedgerEntryDto[]>([]);
  const [isQuickTransferOpen, setIsQuickTransferOpen] = useState(false);

  // Active Saga State for Tracker
  const [activeStep, setActiveStep] = useState<SagaStep>('COMMITTED');
  const [activeStatus, setActiveStatus] = useState<TransactionStatus>('COMPLETED');
  const [activeTxId, setActiveTxId] = useState<string | undefined>();
  const [activeIdemKey, setActiveIdemKey] = useState<string | undefined>();

  const loadData = useCallback(async () => {
    try {
      const accList = await accountsApi.list();
      const txList = await paymentsApi.listTransactions();
      const ledgerList = await ledgerApi.listAllEntries();
      setAccounts(accList);
      setTransactions(txList);
      setLedgerEntries(ledgerList);
    } catch (err) {
      console.error('Failed to load data:', err);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData, isMock]);

  const handleToggleMock = () => {
    const nextMode = !isMock;
    setMockMode(nextMode);
    setIsMock(nextMode);
  };

  const handleTransferComplete = (res: TransferResponse) => {
    setActiveTxId(res.transactionId);
    setActiveIdemKey(res.idempotencyKey);
    setActiveStatus(res.status);
    setActiveStep(res.currentStep);
    setIsQuickTransferOpen(false);
    loadData();
  };

  const handleSagaStepChange = (step: SagaStep, status: TransactionStatus) => {
    setActiveStep(step);
    setActiveStatus(status);
  };

  const titles: Record<NavTab, string> = {
    'admin-portal': 'Trung Tâm Quản Trị & Tần Suất Giao Dịch',
    'user-portal': 'Cổng Khách Hàng Cá Nhân & Sao Kê',
    overview: 'Executive Dashboard',
    accounts: 'Accounts & Treasury',
    transfer: 'Payment Saga Studio',
    ledger: 'Double-Entry Ledger Audit',
    fraud: 'Fraud & Risk Intelligence',
    telemetry: 'System Telemetry & Architecture',
  };

  return (
    <div className="flex h-screen w-screen overflow-hidden bg-[#080C14]">
      {/* Navigation Sidebar */}
      <Sidebar
        activeTab={activeTab}
        onTabChange={setActiveTab}
        isMockMode={isMock}
      />

      {/* Main Workspace */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        <Navbar
          activeTabTitle={titles[activeTab]}
          isMockMode={isMock}
          onToggleMockMode={handleToggleMock}
          onQuickTransfer={() => setIsQuickTransferOpen(true)}
          onRefreshData={loadData}
        />

        <main className="flex-1 overflow-y-auto p-6 space-y-6">
          {activeTab === 'admin-portal' && (
            <AdminPortal
              accounts={accounts}
              transactions={transactions}
              ledgerEntries={ledgerEntries}
              onRefresh={loadData}
            />
          )}

          {activeTab === 'user-portal' && (
            <UserPortal
              accounts={accounts}
              onRefresh={loadData}
            />
          )}

          {activeTab === 'overview' && (
            <DashboardOverview
              accounts={accounts}
              transactions={transactions}
              ledgerEntries={ledgerEntries}
              activeStep={activeStep}
              activeStatus={activeStatus}
              activeTxId={activeTxId}
              activeIdemKey={activeIdemKey}
            />
          )}

          {activeTab === 'accounts' && (
            <AccountsHub accounts={accounts} onRefresh={loadData} />
          )}

          {activeTab === 'transfer' && (
            <TransferStudio
              accounts={accounts}
              transactions={transactions}
              activeStep={activeStep}
              activeStatus={activeStatus}
              activeTxId={activeTxId}
              activeIdemKey={activeIdemKey}
              onTransferComplete={handleTransferComplete}
              onSagaStepChange={handleSagaStepChange}
            />
          )}

          {activeTab === 'ledger' && (
            <LedgerExplorer entries={ledgerEntries} />
          )}

          {activeTab === 'fraud' && <FraudConsole />}

          {activeTab === 'telemetry' && <SystemStatus />}
        </main>
      </div>

      {/* Quick Transfer Dialog */}
      <Modal
        isOpen={isQuickTransferOpen}
        onClose={() => setIsQuickTransferOpen(false)}
        title="Quick Funds Transfer (Saga Orchestration)"
      >
        <TransferTerminal
          accounts={accounts}
          onTransferComplete={handleTransferComplete}
          onSagaStepChange={handleSagaStepChange}
        />
      </Modal>
    </div>
  );
}

export default App;
