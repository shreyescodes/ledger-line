# ADR-0008: Hexagonal service structure enforced by ArchUnit

- **Status:** Proposed
- **Date:** 2026-11-06
- **Stage:** 1

## Context

The services will hold the hardest code in the project: ledger postings, sagas
and reconciliation. That code must be testable without Spring or a database,
must not quietly depend on another service's internals, and must respect
invariants that a compiler can't see. Invariant I1 bans floating-point money;
I10 requires time to come from an injected `Clock`. The brief asks for ArchUnit
module-boundary rules.

## Decision

1. **Packages.** Each service lives in `dev.ledgerline.<service>`:
   - `domain`: plain Java business rules (money, accounts, postings);
   - `application`: use cases that orchestrate the domain through ports;
   - `adapter`: web controllers, persistence, Kafka, PSP clients;
   - `config`: Spring wiring, and the one place allowed to read the system
     clock.

   `libs/common` (`dev.ledgerline.common`) is plain Java shared by all services.
2. **Rules,** shared from `libs/test-support` and run by every service's
   `ArchitectureTest`:
   - a service depends only on itself, `common` and test support, never on
     another service;
   - `domain` doesn't depend on Spring, Jakarta, Jackson, Hibernate, jOOQ or
     outer layers;
   - layers point inwards: adapters → application → domain, and only `config`
     touches everything;
   - no `float`/`double` fields in `domain` (I1);
   - no `Instant.now()`, `LocalDateTime.now()`, `System.currentTimeMillis()`,
     `Clock.systemUTC()`… outside `config` (I10);
   - `libs/common` is framework-free.
3. **The rules are tested too.** `LedgerlineArchitectureRulesTest` checks that
   each rule rejects a fixture class that breaks it and accepts one that
   follows it, so a rule can't pass silently.
4. Services also can't see each other at build time: no service has a Gradle
   dependency on another. The ArchUnit rule is a second line of defence.

## Alternatives considered

- **Layer by technical type** (`controller`, `service`, `repository`):
  familiar, but the domain ends up importing persistence and web types.
- **Java modules (JPMS)** for boundaries: real compile-time encapsulation, but
  awkward with Spring's reflection and test tooling today.
- **Spring Modulith**: good for a modular monolith. Here the boundaries are
  separate deployable services, so it adds little.

## Consequences

- Domain code is unit-testable with plain JUnit, and jqwik property tests
  (Stage 2) run in milliseconds.
- Every class that needs the time takes a `Clock`, so tests can freeze or move
  time (expiry, retries, settlement cut-offs).
- The rules allow empty matches while the skeleton has no domain code. Once
  Stage 2 adds it, they bite.
