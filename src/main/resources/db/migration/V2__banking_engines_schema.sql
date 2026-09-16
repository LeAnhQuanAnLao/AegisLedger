-- ==============================================================================
-- AEGISLEDGER SCHEMA MIGRATION V2: SAVINGS, LENDING, FEE/LIMIT & EOD ENGINES
-- ==============================================================================

-- 1. Seed Well-Known System Accounts for Double-Entry Bookkeeping
INSERT INTO accounts (id, account_number, holder_name, balance, locked_balance, available_balance, currency, status, created_at, updated_at, version)
VALUES 
    ('00000000-0000-0000-0000-000000000001', 'SYS-TREASURY', 'System Bank Treasury', 10000000.0000, 0.0000, 10000000.0000, 'USD', 'ACTIVE', NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000000002', 'SYS-INTEREST-EXP', 'System Interest Expense', 10000000.0000, 0.0000, 10000000.0000, 'USD', 'ACTIVE', NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000000003', 'SYS-INTEREST-INC', 'System Interest Income', 0.0000, 0.0000, 0.0000, 'USD', 'ACTIVE', NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000000004', 'SYS-FEE-REV', 'System Fee Revenue', 0.0000, 0.0000, 0.0000, 'USD', 'ACTIVE', NOW(), NOW(), 0),
    ('00000000-0000-0000-0000-000000000005', 'SYS-SAVINGS-VAULT', 'System Savings Vault', 0.0000, 0.0000, 0.0000, 'USD', 'ACTIVE', NOW(), NOW(), 0)
ON CONFLICT (id) DO NOTHING;

-- 2. Savings Accounts Table
CREATE TABLE savings_accounts (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    savings_number VARCHAR(32) NOT NULL UNIQUE,
    principal_amount NUMERIC(19, 4) NOT NULL,
    interest_rate NUMERIC(7, 4) NOT NULL,
    term_months INT NOT NULL,
    rollover_option VARCHAR(32) NOT NULL,
    accrued_interest NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    start_date DATE NOT NULL,
    maturity_date DATE,
    last_accrual_date DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_savings_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT chk_positive_principal CHECK (principal_amount > 0)
);

CREATE INDEX idx_savings_account_id ON savings_accounts(account_id);
CREATE INDEX idx_savings_status ON savings_accounts(status);
CREATE INDEX idx_savings_maturity ON savings_accounts(maturity_date);

-- 3. Micro-Lending Tables
CREATE TABLE loan_contracts (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    loan_number VARCHAR(32) NOT NULL UNIQUE,
    principal_amount NUMERIC(19, 4) NOT NULL,
    interest_rate NUMERIC(7, 4) NOT NULL,
    term_months INT NOT NULL,
    remaining_principal NUMERIC(19, 4) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    disbursed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_loan_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT chk_positive_loan_principal CHECK (principal_amount > 0)
);

CREATE INDEX idx_loan_account_id ON loan_contracts(account_id);
CREATE INDEX idx_loan_status ON loan_contracts(status);

CREATE TABLE loan_repayment_schedules (
    id UUID PRIMARY KEY,
    loan_id UUID NOT NULL,
    installment_number INT NOT NULL,
    due_date DATE NOT NULL,
    principal_due NUMERIC(19, 4) NOT NULL,
    interest_due NUMERIC(19, 4) NOT NULL,
    total_due NUMERIC(19, 4) NOT NULL,
    principal_paid NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    interest_paid NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_schedule_loan FOREIGN KEY (loan_id) REFERENCES loan_contracts(id)
);

CREATE INDEX idx_schedule_loan_id ON loan_repayment_schedules(loan_id);
CREATE INDEX idx_schedule_due_date ON loan_repayment_schedules(due_date, status);

-- 4. Daily Limit & Usage Tables
CREATE TABLE daily_limit_configs (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL UNIQUE,
    daily_limit NUMERIC(19, 4) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_limit_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT chk_positive_daily_limit CHECK (daily_limit >= 0)
);

CREATE TABLE daily_limit_usages (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    usage_date DATE NOT NULL,
    total_spent NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_usage_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT uq_account_usage_date UNIQUE (account_id, usage_date)
);

CREATE INDEX idx_daily_usage_acc_date ON daily_limit_usages(account_id, usage_date);

-- 5. Daily Accounting Balance Sheet Table (EOD Engine)
CREATE TABLE daily_balance_sheets (
    id UUID PRIMARY KEY,
    reconciliation_date DATE NOT NULL UNIQUE,
    total_accounts_checked INT NOT NULL,
    total_account_balance NUMERIC(19, 4) NOT NULL,
    total_locked_balance NUMERIC(19, 4) NOT NULL,
    total_available_balance NUMERIC(19, 4) NOT NULL,
    total_ledger_debits NUMERIC(19, 4) NOT NULL,
    total_ledger_credits NUMERIC(19, 4) NOT NULL,
    ledger_balanced BOOLEAN NOT NULL,
    discrepancy_count INT NOT NULL DEFAULT 0,
    discrepancies_detail TEXT,
    status VARCHAR(30) NOT NULL,
    execution_duration_ms BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_balance_sheet_date ON daily_balance_sheets(reconciliation_date);
