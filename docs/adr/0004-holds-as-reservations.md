# ADR-0004: Model holds as reservations, not ledger entries

- **Status:** Accepted
- **Date:** 2026-11-06
- **Stage:** 0

## Context

The payment saga reserves the payer's money (`FUNDS_HELD`) before it asks the
PSP, and that can take seconds, or days if the outcome is unknown. The brief
asks for holds (authorize/capture/void) and for a ledger balance that is
separate from the available balance. Entries are immutable (invariant 3), and
every transaction must balance (invariant 2).

## Decision

1. A **hold** is a row in its own table: `id`, `tenant_id`, `account_id`,
   `amount`, `status` (ACTIVE, CAPTURED, VOIDED, EXPIRED), `reference` (the
   payment), `expires_at` (nullable), timestamps. It creates **no entries**.
2. **Balances.** `held = Σ amount of ACTIVE holds`, and
   `available = ledger − held`. The stored balance projection keeps `ledger`
   and `held` together, updated in the same database transaction as the
   change that moves them.
3. **Authorize** requires `available ≥ amount` on every account type.
4. **Capture** posts a balanced transaction that debits the held account by
   at most the hold amount, and marks the hold CAPTURED, in **one** database
   transaction. A partial capture releases the remainder.
5. **Void** and **expiry** release the hold without entries. A hold is
   resolved exactly once.
6. **Expiry** applies only to holds whose payment was never sent to the PSP.
   A hold behind a submitted or unknown payment has no expiry
   ([ADR-0006](0006-psp-timeouts-are-unknown-outcomes.md)).
7. For non-negative accounts, the invariant is `available ≥ 0` (which implies
   `ledger ≥ 0`). A direct debit cannot spend money that a hold reserves.

## Alternatives considered

- **Pending entries that later flip to posted** (some commercial ledgers do
  this): the entry changes state, which violates immutability.
- **Move held funds into a "held funds" account with real entries**
  (authorize = Dr wallet / Cr held-funds; capture = Dr held-funds / Cr payee):
  everything stays double-entry, but the wallet's *ledger* balance drops at
  authorize time. That erases the ledger-vs-available distinction the brief
  asks for. It also doubles the postings per payment and makes the
  held-funds account a hot row.

## Consequences

- Holds can never unbalance the books, because they aren't entries.
- Invariant checks must include holds. The verifier also proves
  `held == Σ ACTIVE holds` per account.
- Authorize, capture and a direct debit on the same account need the same
  concurrency control as postings (Stage 2's locking strategy covers holds
  too).
