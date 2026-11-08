-- Ledger tables (Stage 2, ADR-0013). Every table carries tenant_id (invariant I9), and the
-- composite foreign keys make it impossible for an entry to point at another tenant's account
-- or at an account in a different currency.

CREATE TABLE account (
    id          uuid        PRIMARY KEY,
    tenant_id   uuid        NOT NULL,
    type        text        NOT NULL CHECK (type IN ('CUSTOMER_WALLET', 'MERCHANT', 'PSP_CLEARING',
                                                     'SETTLEMENT_BANK', 'FEE_REVENUE', 'SUSPENSE', 'WRITE_OFF')),
    currency    char(3)     NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    holder_ref  text,
    created_at  timestamptz NOT NULL,
    UNIQUE (id, tenant_id, currency)
);

-- The cached projection of each account's balance (invariant I4), updated in the same database
-- transaction as the entries. normal_sign and non_negative are copied from the account type so
-- the overdraft rule can be a plain CHECK on this row: the last line of defence for I5.
CREATE TABLE account_balance (
    account_id   uuid        PRIMARY KEY,
    tenant_id    uuid        NOT NULL,
    currency     char(3)     NOT NULL,
    normal_sign  smallint    NOT NULL CHECK (normal_sign IN (-1, 1)),
    non_negative boolean     NOT NULL,
    raw_balance  bigint      NOT NULL DEFAULT 0,
    version      bigint      NOT NULL DEFAULT 0 CHECK (version >= 0),
    updated_at   timestamptz NOT NULL,
    FOREIGN KEY (account_id, tenant_id, currency) REFERENCES account (id, tenant_id, currency),
    CONSTRAINT balance_not_overdrawn CHECK (NOT non_negative OR raw_balance * normal_sign >= 0)
);

CREATE TABLE ledger_transaction (
    id          uuid        PRIMARY KEY,
    tenant_id   uuid        NOT NULL,
    type        text        NOT NULL CHECK (type IN ('TRANSFER', 'PAYMENT', 'TOP_UP', 'REFUND', 'PAYOUT',
                                                     'CHARGEBACK', 'DISPUTE_FEE', 'SETTLEMENT', 'ADJUSTMENT',
                                                     'REVERSAL')),
    reference   text,
    reverses    uuid        UNIQUE,
    recorded_at timestamptz NOT NULL,
    UNIQUE (id, tenant_id),
    FOREIGN KEY (reverses, tenant_id) REFERENCES ledger_transaction (id, tenant_id)
);

CREATE TABLE entry (
    id             uuid    PRIMARY KEY,
    tenant_id      uuid    NOT NULL,
    transaction_id uuid    NOT NULL,
    account_id     uuid    NOT NULL,
    currency       char(3) NOT NULL,
    side           text    NOT NULL CHECK (side IN ('DEBIT', 'CREDIT')),
    amount         bigint  NOT NULL CHECK (amount > 0),
    -- Position of this entry in its account's history: 1, 2, 3... with no gaps or repeats.
    -- Statements page on it (keyset pagination).
    account_seq    bigint  NOT NULL CHECK (account_seq > 0),
    FOREIGN KEY (transaction_id, tenant_id) REFERENCES ledger_transaction (id, tenant_id),
    FOREIGN KEY (account_id, tenant_id, currency) REFERENCES account (id, tenant_id, currency),
    UNIQUE (account_id, account_seq)
);

CREATE INDEX entry_transaction_idx ON entry (transaction_id);
CREATE INDEX account_tenant_idx ON account (tenant_id);
