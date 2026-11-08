-- Runs once when a test Postgres container starts. The service connects as this role, which gets
-- only the privileges its migrations grant (see ledger-service V4__app_role_privileges.sql).
CREATE ROLE app_user LOGIN PASSWORD 'app_password';
