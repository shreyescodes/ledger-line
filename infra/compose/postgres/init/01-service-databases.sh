#!/usr/bin/env bash
# Runs once, when the Postgres data volume is first created.
# One database and one owning role per service (ADR-0009). Services never share a database.
set -euo pipefail

: "${SERVICE_DB_PASSWORD:?SERVICE_DB_PASSWORD must be set}"

for service in ledger payment reconciliation notification; do
    psql --set ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
        --set role="${service}_service" --set db="$service" --set password="$SERVICE_DB_PASSWORD" <<'SQL'
CREATE ROLE :"role" LOGIN PASSWORD :'password';
CREATE DATABASE :"db" OWNER :"role";
REVOKE ALL ON DATABASE :"db" FROM PUBLIC;
SQL
done

# ledger-service runs as a least-privilege role (ADR-0013): its migrations, run as ledger_service,
# grant it only SELECT/INSERT on ledger rows, so it can never UPDATE or DELETE an entry.
psql --set ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres     --set password="$SERVICE_DB_PASSWORD" <<'SQL'
CREATE ROLE ledger_app LOGIN PASSWORD :'password';
GRANT CONNECT ON DATABASE ledger TO ledger_app;
SQL
