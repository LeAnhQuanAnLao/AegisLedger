-- ==============================================================================
-- AEGISLEDGER SCHEMA MIGRATION V1: CORE BANKING & SAGA ORCHESTRATOR
-- ==============================================================================

-- 1. Accounts Table
CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    account_number VARCHAR(32) NOT NULL UNIQUE,
    holder_name VARCHAR(128) NOT NULL,
    balance NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    locked_balance NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    available_balance NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_positive_balance CHECK (balance >= 0),
    CONSTRAINT chk_positive_locked CHECK (locked_balance >= 0),
    CONSTRAINT chk_positive_available CHECK (available_balance >= 0)
);

CREATE INDEX idx_accounts_number ON accounts(account_number);

-- 2. Transactions Table
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    source_account_id UUID NOT NULL,
    destination_account_id UUID NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    saga_step VARCHAR(30) NOT NULL,
    failure_reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_tx_source_account FOREIGN KEY (source_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_tx_dest_account FOREIGN KEY (destination_account_id) REFERENCES accounts(id)
);

CREATE INDEX idx_tx_status ON transactions(status);
CREATE INDEX idx_tx_source_acc ON transactions(source_account_id);
CREATE INDEX idx_tx_dest_acc ON transactions(destination_account_id);

-- 3. Double-Entry Immutable Ledger Entries Table
CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    account_id UUID NOT NULL,
    entry_type VARCHAR(10) NOT NULL, -- 'DEBIT' or 'CREDIT'
    amount NUMERIC(19, 4) NOT NULL,
    balance_after NUMERIC(19, 4) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ledger_tx FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    CONSTRAINT fk_ledger_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT chk_positive_amount CHECK (amount > 0)
);

CREATE INDEX idx_ledger_tx_id ON ledger_entries(transaction_id);
CREATE INDEX idx_ledger_account_id ON ledger_entries(account_id);
CREATE INDEX idx_ledger_created_at ON ledger_entries(created_at);

-- 4. Transactional Outbox Events Table
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_outbox_status ON outbox_events(status, created_at);

-- 5. Idempotency Records Table
CREATE TABLE idempotency_records (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    status_code INT,
    response_body TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_idempotency_key ON idempotency_records(idempotency_key);
