# ADR-0011: Time-ordered UUIDv7 ids, made by the application

- **Status:** Proposed
- **Date:** 2026-11-08
- **Stage:** 2

## Context

Invariant I10 asks for time-ordered ids (UUIDv7 or ULID) from a library, UTC
`Instant`s and an injected `Clock`. Ids are primary keys of tables that grow
for ever (entries), they appear in URLs and events, and services create them
before talking to each other (a payment id doubles as the PSP idempotency key,
ADR-0006).

## Decision

1. **UUIDv7 (RFC 9562)**, stored in Postgres as the native `uuid` type
   (16 bytes). The first 48 bits are a millisecond timestamp, so new ids land
   at the right-hand end of B-tree indexes instead of at random pages.
2. **Generated in the application**, not by the database, through the
   `IdGenerator` interface in `libs/common`. A service knows an id before it
   writes anything, which idempotent retries need.
3. **Library:** `com.github.f4b6a3:uuid-creator` 6.1.1, configured with
   `withIncrementPlus1()`, so ids made in the same millisecond still sort in
   creation order. It is a small library with no dependencies, dedicated to
   RFC 9562.
4. **The clock is injected.** `TimeOrderedIdGenerator` takes the service's
   `Clock` bean, so tests can pin time (I10).
5. **Ids are not a source of ordering truth.** Ids from concurrent
   transactions can commit out of order. Anything that needs a gap-free order
   uses an explicit sequence instead, such as `entry.account_seq` for
   statements (ADR-0013) and, later, the outbox's own sequence.

## Alternatives considered

- **`bigserial` keys**: compact and ordered, but the database hands them out,
  they leak volumes, and they can't be made before the insert.
- **UUIDv4**: random, which scatters index inserts and gives no time order.
- **ULID**: also time-ordered, but needs a text or custom binary column.
  UUIDv7 is the standard form of the same idea and fits Postgres's `uuid`
  type.
- **Postgres 18's `uuidv7()`**: works, but makes the database the id source,
  so the application can't know the id before the round trip.
- **Java's own UUIDv7**: the JDK has none in Java 25.

## Consequences

- One small runtime dependency in `libs/common`.
- A UUIDv7 reveals its creation time to anyone who sees it. That's fine for
  this system, whose ids aren't secret. Authorization never relies on an id
  being hard to guess.
