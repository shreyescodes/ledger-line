# ADR-0012: Plain SQL with Spring's JdbcClient for the ledger

- **Status:** Proposed
- **Date:** 2026-11-08
- **Stage:** 2

## Context

The brief says the ledger hot path uses Spring Data JDBC or jOOQ, with explicit
SQL and locking, and asks for the choice in an ADR. Postings need exact control
over what runs: `SELECT ... FOR UPDATE OF b` in a fixed order, conditional
updates (`WHERE version = ?`), isolation levels per transaction, and batched
inserts. Some statements also exist only for the benchmark's three strategies.

## Decision

1. **Spring JDBC (`JdbcClient`, plus `JdbcTemplate` for batch inserts)**, with
   the SQL written out by hand in the persistence adapters. No ORM and no
   generated query DSL.
2. **Transactions are explicit.** Each posting strategy builds its own
   `TransactionTemplate` with the isolation level it needs, so the retry loop
   sits *outside* the database transaction and every attempt starts fresh.
3. Mapping is done by small row-mapper lambdas into the domain's records.
4. Spring Data JDBC's repository layer is not used for the ledger. Its derived
   queries hide exactly the SQL the ledger needs to control. It stays an
   option for simpler services.

## Alternatives considered

- **jOOQ**: type-safe SQL and a good fit in principle. But it needs code
  generation from a live schema (Testcontainers at build time, or its DDL
  parser), which adds a build step and dependencies for about a dozen
  statements. Worth revisiting if the ledger's SQL grows a lot.
- **Spring Data JDBC repositories**: convenient CRUD, but locking clauses,
  conditional updates and per-call isolation would all be custom `@Query`
  strings anyway.
- **JPA/Hibernate**: dirty checking, flush ordering and lazy loading make it
  hard to see which statements run and when. That is exactly what a
  concurrency-critical ledger must not hide.

## Consequences

- Every statement the ledger runs is visible in code review, and in Postgres
  logs as written.
- SQL typos surface at test time, not compile time. The Testcontainers
  integration tests exercise every statement against real Postgres.
- A few type conversions are manual: `Instant` is written as
  `java.sql.Timestamp` and `uuid` is read with `getObject(..., UUID.class)`.
