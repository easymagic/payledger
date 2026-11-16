-- Milestone 3: webhook endpoints + transactional outbox deliveries.

CREATE TABLE webhook_endpoints (
    id              UUID            PRIMARY KEY,
    merchant_id     UUID            NOT NULL REFERENCES merchants (id),
    url             VARCHAR(500)    NOT NULL,
    secret          VARCHAR(100)    NOT NULL,
    event_types     VARCHAR(200)    NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_webhook_endpoints_merchant_id ON webhook_endpoints (merchant_id);

CREATE TABLE webhook_deliveries (
    id                  UUID            PRIMARY KEY,
    endpoint_id         UUID            NOT NULL REFERENCES webhook_endpoints (id),
    event_type          VARCHAR(64)     NOT NULL,
    payload             TEXT            NOT NULL,
    status              VARCHAR(32)     NOT NULL,
    attempt_count       INTEGER         NOT NULL DEFAULT 0,
    max_attempts        INTEGER         NOT NULL DEFAULT 5,
    next_attempt_at     TIMESTAMPTZ     NOT NULL,
    last_error          VARCHAR(1000),
    transaction_id      UUID,
    created_at          TIMESTAMPTZ     NOT NULL,
    updated_at          TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_webhook_deliveries_status_next
    ON webhook_deliveries (status, next_attempt_at);

CREATE INDEX idx_webhook_deliveries_endpoint_id
    ON webhook_deliveries (endpoint_id);
