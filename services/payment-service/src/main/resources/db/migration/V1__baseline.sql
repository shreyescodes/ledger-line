-- Baseline for the payment database. Payment tables arrive in Stages 3 and 5.
-- New sessions in this database run in UTC (invariant I10).
ALTER DATABASE ${flyway:database} SET timezone TO 'UTC';
