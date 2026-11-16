-- Milestone 2: link refunds to the original transfer (still one ledger table).

ALTER TABLE transactions
    ADD COLUMN related_transaction_id UUID REFERENCES transactions (id);

CREATE INDEX idx_transactions_related_transaction_id
    ON transactions (related_transaction_id);

COMMENT ON COLUMN transactions.related_transaction_id IS
    'For REFUND rows, points at the original TRANSFER (or other) ledger entry being reversed.';
