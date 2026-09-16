import {
  AccountDto,
  ApiResponse,
  CreateAccountRequest,
  LedgerEntryDto,
  PageResponse,
  Transaction,
  TransferRequest,
  TransferResponse,
} from '../types/banking';
import {
  INITIAL_MOCK_ACCOUNTS,
  INITIAL_MOCK_LEDGER_ENTRIES,
  INITIAL_MOCK_TRANSACTIONS,
} from './mockData';
import { generateUUID } from '../utils/formatters';

const BASE_URL = '';

let mockMode = true;
let mockAccounts = [...INITIAL_MOCK_ACCOUNTS];
let mockTransactions = [...INITIAL_MOCK_TRANSACTIONS];
let mockLedgerEntries = [...INITIAL_MOCK_LEDGER_ENTRIES];

export function getMockMode(): boolean {
  return mockMode;
}

export function setMockMode(enabled: boolean): void {
  mockMode = enabled;
}

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<ApiResponse<T>> {
  const response = await fetch(`${BASE_URL}${endpoint}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || `HTTP ${response.status}: Failed request`);
  }
  return data;
}

export const accountsApi = {
  async create(req: CreateAccountRequest): Promise<ApiResponse<AccountDto>> {
    if (mockMode) {
      const newAcc: AccountDto = {
        id: generateUUID(),
        accountNumber: req.accountNumber,
        holderName: req.holderName,
        balance: req.initialDeposit,
        lockedBalance: 0,
        availableBalance: req.initialDeposit,
        currency: req.currency,
        status: 'ACTIVE',
        createdAt: new Date().toISOString(),
      };
      mockAccounts = [newAcc, ...mockAccounts];
      return { success: true, code: 'SUCCESS', message: 'Account created', data: newAcc, timestamp: new Date().toISOString() };
    }
    return request<AccountDto>('/api/v1/accounts', {
      method: 'POST',
      body: JSON.stringify(req),
    });
  },

  async getById(id: string): Promise<ApiResponse<AccountDto>> {
    if (mockMode) {
      const acc = mockAccounts.find((a) => a.id === id);
      if (!acc) throw new Error('Account not found');
      return { success: true, code: 'SUCCESS', message: 'OK', data: acc, timestamp: new Date().toISOString() };
    }
    return request<AccountDto>(`/api/v1/accounts/${id}`);
  },

  async getByNumber(accNumber: string): Promise<ApiResponse<AccountDto>> {
    if (mockMode) {
      const acc = mockAccounts.find((a) => a.accountNumber === accNumber);
      if (!acc) throw new Error('Account not found');
      return { success: true, code: 'SUCCESS', message: 'OK', data: acc, timestamp: new Date().toISOString() };
    }
    return request<AccountDto>(`/api/v1/accounts/number/${accNumber}`);
  },

  async list(): Promise<AccountDto[]> {
    return mockAccounts;
  },
};

export const paymentsApi = {
  async transfer(req: TransferRequest): Promise<ApiResponse<TransferResponse>> {
    if (mockMode) {
      // Simulate saga processing
      const source = mockAccounts.find((a) => a.id === req.sourceAccountId);
      const dest = mockAccounts.find((a) => a.id === req.destinationAccountId);

      if (!source || !dest) throw new Error('Source or Destination account does not exist');
      if (source.availableBalance < req.amount) throw new Error('Insufficient funds in source account');

      // Fraud rule check: amount > 100,000 triggers compensation demo if requested
      const isFraud = req.amount > 100000 && req.description?.includes('TRIGGER_FRAUD');

      const txId = generateUUID();
      if (isFraud) {
        const failedTx: Transaction = {
          id: txId,
          idempotencyKey: req.idempotencyKey,
          sourceAccountId: req.sourceAccountId,
          destinationAccountId: req.destinationAccountId,
          amount: req.amount,
          currency: req.currency,
          status: 'COMPENSATED',
          sagaStep: 'COMPENSATED',
          failureReason: 'Fraud Engine: Transaction amount exceeded risk thresholds. Funds released.',
          createdAt: new Date().toISOString(),
        };
        mockTransactions = [failedTx, ...mockTransactions];
        return {
          success: false,
          code: 'FRAUD_DETECTED',
          message: 'Transaction rolled back by Saga Compensator',
          data: {
            transactionId: txId,
            idempotencyKey: req.idempotencyKey,
            status: 'COMPENSATED',
            currentStep: 'COMPENSATED',
            message: 'Funds released. Saga Compensated.',
            timestamp: new Date().toISOString(),
          },
          timestamp: new Date().toISOString(),
        };
      }

      // Successful Transfer: Update balances and add ledger entries
      source.balance -= req.amount;
      source.availableBalance -= req.amount;
      dest.balance += req.amount;
      dest.availableBalance += req.amount;

      const completedTx: Transaction = {
        id: txId,
        idempotencyKey: req.idempotencyKey,
        sourceAccountId: req.sourceAccountId,
        destinationAccountId: req.destinationAccountId,
        amount: req.amount,
        currency: req.currency,
        status: 'COMPLETED',
        sagaStep: 'COMMITTED',
        createdAt: new Date().toISOString(),
      };
      mockTransactions = [completedTx, ...mockTransactions];

      // Add 2 balanced ledger entries (Double-entry)
      mockLedgerEntries = [
        {
          id: generateUUID(),
          transactionId: txId,
          accountId: source.id,
          entryType: 'DEBIT',
          amount: req.amount,
          balanceAfter: source.balance,
          description: `Debit transfer to ${dest.accountNumber}`,
          createdAt: new Date().toISOString(),
        },
        {
          id: generateUUID(),
          transactionId: txId,
          accountId: dest.id,
          entryType: 'CREDIT',
          amount: req.amount,
          balanceAfter: dest.balance,
          description: `Credit transfer from ${source.accountNumber}`,
          createdAt: new Date().toISOString(),
        },
        ...mockLedgerEntries,
      ];

      return {
        success: true,
        code: 'SUCCESS',
        message: 'Transfer completed',
        data: {
          transactionId: txId,
          idempotencyKey: req.idempotencyKey,
          status: 'COMPLETED',
          currentStep: 'COMMITTED',
          message: 'Funds successfully transferred and ledger balanced',
          timestamp: new Date().toISOString(),
        },
        timestamp: new Date().toISOString(),
      };
    }

    return request<TransferResponse>('/api/v1/payments/transfer', {
      method: 'POST',
      headers: { 'Idempotency-Key': req.idempotencyKey },
      body: JSON.stringify(req),
    });
  },

  async getTransaction(id: string): Promise<ApiResponse<Transaction>> {
    if (mockMode) {
      const tx = mockTransactions.find((t) => t.id === id);
      if (!tx) throw new Error('Transaction not found');
      return { success: true, code: 'SUCCESS', message: 'OK', data: tx, timestamp: new Date().toISOString() };
    }
    return request<Transaction>(`/api/v1/payments/${id}`);
  },

  async listTransactions(): Promise<Transaction[]> {
    return mockTransactions;
  },

  async listPaged(page = 0, size = 20): Promise<ApiResponse<PageResponse<Transaction>>> {
    if (mockMode) {
      return {
        success: true,
        code: 'SUCCESS',
        message: 'OK',
        data: {
          content: mockTransactions.slice(page * size, (page + 1) * size),
          totalElements: mockTransactions.length,
          totalPages: Math.ceil(mockTransactions.length / size) || 1,
          number: page,
          size,
        },
        timestamp: new Date().toISOString(),
      };
    }
    return request<PageResponse<Transaction>>(`/api/v1/payments?page=${page}&size=${size}`);
  },
};

export const ledgerApi = {
  async getAccountEntries(accountId: string, page = 0, size = 20): Promise<ApiResponse<PageResponse<LedgerEntryDto>>> {
    if (mockMode) {
      const entries = mockLedgerEntries.filter((e) => e.accountId === accountId);
      return {
        success: true,
        code: 'SUCCESS',
        message: 'OK',
        data: {
          content: entries.slice(page * size, (page + 1) * size),
          totalElements: entries.length,
          totalPages: Math.ceil(entries.length / size) || 1,
          number: page,
          size,
        },
        timestamp: new Date().toISOString(),
      };
    }
    return request<PageResponse<LedgerEntryDto>>(`/api/v1/ledger/${accountId}/entries?page=${page}&size=${size}`);
  },

  async getByTransactionId(txId: string): Promise<ApiResponse<LedgerEntryDto[]>> {
    if (mockMode) {
      const entries = mockLedgerEntries.filter((e) => e.transactionId === txId);
      return { success: true, code: 'SUCCESS', message: 'OK', data: entries, timestamp: new Date().toISOString() };
    }
    return request<LedgerEntryDto[]>(`/api/v1/ledger/transaction/${txId}`);
  },

  async listAllEntries(): Promise<LedgerEntryDto[]> {
    return mockLedgerEntries;
  },
};
