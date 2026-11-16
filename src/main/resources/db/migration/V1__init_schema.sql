-- PayLedger Milestone 1 schema
-- Matches JPA entities: Merchant, Customer, Wallet, LedgerTransaction (+ Money embeddable)

CREATE TABLE merchants (
    id              UUID            PRIMARY KEY,
    name            VARCHAR(200)    NOT NULL,
    email           VARCHAR(255)    NOT NULL,
    status          VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL,
    CONSTRAINT uk_merchants_email UNIQUE (email)
);

CREATE TABLE customers (
    id              UUID            PRIMARY KEY,
    merchant_id     UUID            NOT NULL REFERENCES merchants (id),
    external_ref    VARCHAR(100)    NOT NULL,
    name            VARCHAR(200)    NOT NULL,
    email           VARCHAR(255)    NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL,
    CONSTRAINT uk_customers_merchant_external_ref UNIQUE (merchant_id, external_ref)
);

CREATE INDEX idx_customers_merchant_id ON customers (merchant_id);
CREATE INDEX idx_customers_email ON customers (email);

CREATE TABLE wallets (
    id                  UUID            PRIMARY KEY,
    customer_id         UUID            NOT NULL REFERENCES customers (id),
    balance_amount      NUMERIC(19, 4)  NOT NULL,
    balance_currency    VARCHAR(3)      NOT NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL,
    updated_at          TIMESTAMPTZ     NOT NULL,
    CONSTRAINT uk_wallets_customer_id UNIQUE (customer_id),
    CONSTRAINT chk_wallets_balance_non_negative CHECK (balance_amount >= 0)
);

CREATE TABLE transactions (
    id                      UUID            PRIMARY KEY,
    wallet_id               UUID            NOT NULL REFERENCES wallets (id),
    counterparty_wallet_id  UUID            REFERENCES wallets (id),
    type                    VARCHAR(32)     NOT NULL,
    status                  VARCHAR(32)     NOT NULL,
    amount                  NUMERIC(19, 4)  NOT NULL,
    currency                VARCHAR(3)      NOT NULL,
    idempotency_key         VARCHAR(100)    NOT NULL,
    description             VARCHAR(500),
    correlation_id          UUID,
    created_at              TIMESTAMPTZ     NOT NULL,
    updated_at              TIMESTAMPTZ     NOT NULL,
    CONSTRAINT uk_transactions_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT chk_transactions_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_transactions_wallet_id ON transactions (wallet_id);
CREATE INDEX idx_transactions_type ON transactions (type);
CREATE INDEX idx_transactions_status ON transactions (status);
CREATE INDEX idx_transactions_created_at ON transactions (created_at);
CREATE INDEX idx_transactions_correlation_id ON transactions (correlation_id);
