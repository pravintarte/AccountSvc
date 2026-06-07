CREATE TABLE IF NOT EXISTS account_transaction (
    event_id UUID PRIMARY KEY,
    account_id VARCHAR(128) NOT NULL,
    transaction_type VARCHAR(16) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    event_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    idempotency_key VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_account_transaction_account_id
    ON account_transaction(account_id);

CREATE INDEX IF NOT EXISTS idx_account_transaction_account_timestamp
    ON account_transaction(account_id, event_timestamp);
