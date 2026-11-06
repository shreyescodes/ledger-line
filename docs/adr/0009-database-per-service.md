# ADR-0009: One database per service, migrated by its owner

- **Status:** Proposed
- **Date:** 2026-11-06
- **Stage:** 1

## Context

Ledger, payment, reconciliation and notification each keep state. If services
share tables, any one of them can break another's invariants. For example, a
payment-service query could update ledger entries that must be immutable (I3).
Services also need to evolve their schemas independently, with
expand/contract migrations in Stage 11.

## Decision

1. **Each stateful service owns one Postgres database** (`ledger`, `payment`,
   `reconciliation`, `notification`), plus a role that owns it
   (`<service>_service`). No service connects to another service's database.
   Data crosses services only through APIs and events.
2. **Each service migrates its own schema with Flyway** at startup, from
   `src/main/resources/db/migration`. The Stage 1 baseline only sets the
   database's time zone to UTC (I10).
3. **Locally, one Postgres instance hosts all the databases.** An init script
   creates them once, when the volume is created. In AWS (Stage 11) this can
   become one RDS instance with separate databases, or separate instances,
   decided there on cost.
4. **Integration tests run against real Postgres in Testcontainers**, using the
   same image version as Compose (`postgres:18.6-alpine`). Never an in-memory
   database.
5. In Stage 1 only `ledger-service` and `payment-service` connect to their
   databases. Reconciliation and notification wire up theirs in the stages that
   need them.

## Alternatives considered

- **One shared database with a schema per service**: cheaper, but a single
  role with broad grants can cross schemas. Least privilege (Stage 7) would
  have to undo that.
- **H2 or another in-memory database for tests**: fast, but it doesn't behave
  like Postgres in exactly the areas we test (locking, isolation levels,
  deferred constraints, row-level security).

## Consequences

- Cross-service reads go through APIs or events. Reconciliation reads the
  ledger through the ledger-service API or ledger events, not with SQL.
- Least-privilege roles (separate migration and app roles, no UPDATE/DELETE on
  entries) come in Stages 2 and 7, on top of this split.
