-- Milestone 4: local demo users for JWT auth (passwords seeded at runtime via BCrypt).

CREATE TABLE users (
    id              UUID            PRIMARY KEY,
    username        VARCHAR(100)    NOT NULL,
    password_hash   VARCHAR(100)    NOT NULL,
    role            VARCHAR(32)     NOT NULL,
    merchant_id     UUID            REFERENCES merchants (id),
    enabled         BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT chk_users_merchant_role CHECK (
        (role = 'ADMIN' AND merchant_id IS NULL)
        OR (role = 'MERCHANT' AND merchant_id IS NOT NULL)
    )
);
