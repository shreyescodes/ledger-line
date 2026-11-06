-- Baseline for the ledger database. The ledger tables arrive in Stage 2.
-- New sessions in this database run in UTC (invariant I10).
ALTER DATABASE ${flyway:database} SET timezone TO 'UTC';
