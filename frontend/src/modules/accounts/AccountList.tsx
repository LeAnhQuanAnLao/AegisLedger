import React, { useState } from 'react';
import { AccountDto } from '../../core/types/banking';
import { AccountCard } from './AccountCard';
import { CreateAccountModal } from './CreateAccountModal';
import { Button } from '../../components/ui/Button';
import { Plus, Search } from 'lucide-react';

interface AccountListProps {
  accounts: AccountDto[];
  selectedAccountId?: string;
  onSelectAccount?: (account: AccountDto) => void;
  onRefresh: () => void;
}

export const AccountList: React.FC<AccountListProps> = ({
  accounts,
  selectedAccountId,
  onSelectAccount,
  onRefresh,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);

  const filteredAccounts = accounts.filter(
    (acc) =>
      acc.accountNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
      acc.holderName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="relative flex-1 max-w-md">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Search by account number or holder..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full bg-slate-900/90 border border-slate-800 rounded-lg pl-9 pr-4 py-2 text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-indigo-500"
          />
        </div>

        <Button
          variant="primary"
          icon={<Plus className="w-4 h-4" />}
          onClick={() => setIsModalOpen(true)}
        >
          Open Account
        </Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {filteredAccounts.map((account) => (
          <AccountCard
            key={account.id}
            account={account}
            isSelected={account.id === selectedAccountId}
            onSelect={onSelectAccount}
          />
        ))}

        {filteredAccounts.length === 0 && (
          <div className="col-span-2 py-12 text-center text-slate-500 text-sm">
            No accounts matched your search criteria.
          </div>
        )}
      </div>

      <CreateAccountModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={onRefresh}
      />
    </div>
  );
};
