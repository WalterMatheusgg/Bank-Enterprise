-- Demo-only credit operations. Existing V1 migration remains unchanged.
-- Deposits are tracked in a separate audit table; the ledger retains double-entry transfer entries.
CREATE TABLE account_deposits (
  id UUID PRIMARY KEY,
  idempotency_key VARCHAR(120) NOT NULL UNIQUE,
  account_id UUID NOT NULL REFERENCES bank_accounts(id),
  amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_deposit_account_date ON account_deposits(account_id, created_at);
