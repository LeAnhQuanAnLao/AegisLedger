import React, { useState, useEffect } from 'react';
import { AccountDto, DailyLimitStatusDto, TransferResponse } from '../core/types/banking';
import { feeLimitApi } from '../core/api/bankingServicesApi';
import { AccountOverviewCard } from '../modules/user/AccountOverviewCard';
import { UserStatementTable } from '../modules/user/UserStatementTable';
import { UserTransferModal } from '../modules/user/UserTransferModal';
import { UserSavingsLoanSummary } from '../modules/user/UserSavingsLoanSummary';

interface UserPortalProps {
  accounts: AccountDto[];
  onRefresh?: () => void;
}

export const UserPortal: React.FC<UserPortalProps> = ({ accounts, onRefresh }) => {
  const [selectedAccountId, setSelectedAccountId] = useState<string>(
    accounts[0]?.id || ''
  );
  const [limitStatus, setLimitStatus] = useState<DailyLimitStatusDto | null>(null);
  const [isTransferModalOpen, setIsTransferModalOpen] = useState(false);

  useEffect(() => {
    if (accounts.length > 0 && !selectedAccountId) {
      setSelectedAccountId(accounts[0].id);
    }
  }, [accounts, selectedAccountId]);

  useEffect(() => {
    if (!selectedAccountId) return;
    feeLimitApi.getLimitStatus(selectedAccountId)
      .then((res) => setLimitStatus(res.data))
      .catch((err) => console.error('Failed to get limit status', err));
  }, [selectedAccountId]);

  const currentAccount = accounts.find((a) => a.id === selectedAccountId) || accounts[0];

  const handleTransferSuccess = (_res: TransferResponse) => {
    if (onRefresh) onRefresh();
    // Refresh limit status
    if (selectedAccountId) {
      feeLimitApi.getLimitStatus(selectedAccountId).then((r) => setLimitStatus(r.data));
    }
  };

  if (!currentAccount) {
    return (
      <div className="p-12 text-center text-slate-500 font-mono">
        Không có tài khoản nào trong hệ thống. Vui lòng tạo tài khoản mới.
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-fade-in">
      {/* 1. Account Details, Balances & Daily Limit */}
      <AccountOverviewCard
        accounts={accounts}
        selectedAccountId={selectedAccountId}
        onSelectAccount={setSelectedAccountId}
        limitStatus={limitStatus}
        onOpenTransfer={() => setIsTransferModalOpen(true)}
      />

      {/* 2. Personal Account Statement & Transaction History */}
      <UserStatementTable accountId={selectedAccountId} />

      {/* 3. Savings & Loans Summary */}
      <UserSavingsLoanSummary accountId={selectedAccountId} />

      {/* Quick Transfer Dialog with Fee Preview */}
      <UserTransferModal
        isOpen={isTransferModalOpen}
        onClose={() => setIsTransferModalOpen(false)}
        currentAccount={currentAccount}
        accounts={accounts}
        onSuccess={handleTransferSuccess}
      />
    </div>
  );
};
