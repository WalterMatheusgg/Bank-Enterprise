CREATE TABLE bank_accounts (
 id UUID PRIMARY KEY,
 owner_name VARCHAR(160) NOT NULL,
 currency VARCHAR(3) NOT NULL,
 balance NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE bank_transfers (
 id UUID PRIMARY KEY,
 idempotency_key VARCHAR(120) NOT NULL UNIQUE,
 from_account_id UUID NOT NULL REFERENCES bank_accounts(id),
 to_account_id UUID NOT NULL REFERENCES bank_accounts(id),
 amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT distinct_accounts CHECK (from_account_id <> to_account_id)
);
CREATE TABLE ledger_entries (
 id UUID PRIMARY KEY,
 transfer_id UUID NOT NULL REFERENCES bank_transfers(id),
 account_id UUID NOT NULL REFERENCES bank_accounts(id),
 entry_type VARCHAR(6) NOT NULL CHECK (entry_type IN ('DEBIT','CREDIT')),
 amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
 created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_ledger_account ON ledger_entries(account_id, created_at);
CREATE INDEX idx_transfer_from ON bank_transfers(from_account_id);
