# ADR-0013: Ledger schema, and invariants the database enforces itself

- **Status:** Proposed
- **Date:** 2026-11-08
- **Stage:** 2

## Context

The brief requires several invariants to be enforced "in code AND in the
DB":

- I2: balanced transactions, through a deferred constraint trigger;
- I3: immutable entries, through privileges and a trigger;
- I4: a balance projection updated in the same transaction;
- I5: no overdraft, even under concurrency.

Application code has bugs, and people with database access make mistakes. The
database is the last place where those can be stopped.

## Decision

**Tables** (`V2__ledger_tables.sql`): `account`, `account_balance` (the
projection), `ledger_transaction` and `entry`. Every table has `tenant_id`.

**Database-enforced rules:**

| Invariant | How the database enforces it |
|---|---|
| I2: at least two entries; per currency, debits = credits | `CONSTRAINT TRIGGER ... DEFERRABLE INITIALLY DEFERRED` on `entry` and on `ledger_transaction`, checked at commit |
| I2: entry currency = account currency | composite foreign key `entry (account_id, tenant_id, currency) → account (id, tenant_id, currency)` |
| I3: entries and transactions never change | `BEFORE UPDATE OR DELETE` and `BEFORE TRUNCATE` triggers raise errors, even for the owner |
| I3: least privilege | the service connects as an app role with only `SELECT, INSERT` on ledger tables, and `UPDATE` on just the projection's `raw_balance`, `version` and `updated_at` columns. Migrations run as the owner (`V4__app_role_privileges.sql`) |
| I5: no overdraft | `CHECK (NOT non_negative OR raw_balance * normal_sign >= 0)` on `account_balance`; the flag and sign are copied from the account type when the row is created |
| I9 (partly): no cross-tenant links | composite foreign keys include `tenant_id`, so an entry can't point at another tenant's account or transaction. Row-level security comes in Stage 7 |
| Statement order | `entry.account_seq`, unique per account, is the position of the entry in its account's history |

**Balances.** `account_balance.raw_balance` is the debit-positive sum of the
account's entries (ADR-0003), and `version` goes up by one per entry. Every
posting strategy writes the entries and the new balance in one database
transaction (I4). The verifier proves equality continuously (see below).

**Sequence numbers.** Each entry's `account_seq` is the balance's new version,
assigned while the posting owns that balance row. Each strategy makes sure of
that in its own way: a row lock, a version check, or serializable isolation.
Statements page on `account_seq` (keyset pagination). Because the numbers are
gap-free and given out in commit order per account, a reader paging through a
statement can't miss an entry that committed late, which id-based paging
could.

**The verifier** (`InvariantVerifier`, scheduled every minute and exported as
`ledger_invariant_violations{check}`) recounts in one REPEATABLE READ snapshot:

- balance = Σ entries;
- every tenant's books sum to zero per currency;
- no overdrawn non-negative account;
- every transaction balances;
- version = entry count = last sequence number.

## Alternatives considered

- **Checks only in application code**: one bug, or one manual `UPDATE`, and
  the books are silently wrong.
- **Balance computed on read (no projection)**: always correct, but an O(n)
  sum per read, and nothing to lock for overdraft checks.
- **Immediate (non-deferred) balance check**: would force all entries into a
  single multi-row insert. Deferral lets the code insert them in any order.
- **Running balance stored on each entry**: makes statements cheaper, but
  conflicts with splitting hot accounts into sub-balances later (Stage 10).

## Consequences

- Tests can't `TRUNCATE` or `DELETE` ledger rows. Each test uses a fresh
  tenant instead, which also matches how the system really isolates data.
- Corrections must be reversing transactions, as DOMAIN.md §5 already says.
- Two database roles per service. Compose and Testcontainers create the app
  role, and production does the same in Stage 11.
- Each posting costs a few extra trigger checks at commit. The benchmark
  (ADR-0014) measures the strategies with these checks included.
