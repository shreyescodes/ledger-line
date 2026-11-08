-- Least privilege (I3, ADR-0013). Migrations run as the database owner; the service runs as
-- ${app_role}, which may read and append ledger rows but never update or delete them. It may
-- only move the balance projection's figures, not an account's type or flags.
-- The role itself is created by infrastructure (Compose init script, Testcontainers init script).

GRANT USAGE ON SCHEMA public TO ${app_role};
GRANT SELECT, INSERT ON account, ledger_transaction, entry TO ${app_role};
GRANT SELECT, INSERT ON account_balance TO ${app_role};
GRANT UPDATE (raw_balance, version, updated_at) ON account_balance TO ${app_role};
