import React from 'react';
import { AccountDto } from '../core/types/banking';
import { AccountList } from '../modules/accounts/AccountList';

interface AccountsHubProps {
  accounts: AccountDto[];
  onRefresh: () => void;
  onSelectAccount?: (account: AccountDto) => void;
}

export const AccountsHub: React.FC<AccountsHubProps> = ({
  accounts,
  onRefresh,
  onSelectAccount,
}) => {
  return (
    <div className="space-y-6">
      <div>
        <h3 className="text-lg font-bold text-slate-100 tracking-tight">
          Accounts & Treasury Management
        </h3>
        <p className="text-xs text-slate-400 mt-0.5">
          All accounts enforce Pessimistic Locking (`SELECT ... FOR UPDATE`) with zero balance race conditions.
        </p>
      </div>

      <AccountList
        accounts={accounts}
        onRefresh={onRefresh}
        onSelectAccount={onSelectAccount}
      />
    </div>
  );
};
