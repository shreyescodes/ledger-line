# ADR-0014: Ordered row locks prevent overdrafts

- **Status:** Proposed
- **Date:** 2026-11-08
- **Stage:** 2

## Context

Invariant I5: a non-negative account must never go below zero, even when many
postings hit it at once. The danger is the gap between reading a balance
("there is ₹500") and writing the debit. Two postings can both read ₹500 and
both debit ₹400. The brief asks for three strategies to be implemented and
benchmarked, with the winner recorded here.

All three share the same pure posting rules (`PostingRules`) and the same SQL.
They differ only in how they make the read-check-write safe:

| Strategy | How |
|---|---|
| `ORDERED_LOCKS` | `SELECT ... FOR UPDATE OF b` on the touched balance rows, always in account-id order, then check and write. Concurrent postings to the same account wait in line; the fixed order rules out deadlocks. |
| `OPTIMISTIC` | Read without locks, check, then `UPDATE ... WHERE version = ?` on each balance. If any row changed, roll back and retry. |
| `SERIALIZABLE` | Plain reads and writes at `SERIALIZABLE` isolation. Postgres aborts one side of any unsafe interleaving (40001), and that side retries. |

All three proved correct. The 1,000-parallel-transfer test passes for each
strategy with zero invariant violations
([stage-2-ci.md](../results/stage-2-ci.md)). The choice is about cost under
load.

## Evidence

From [stage-2-posting-strategy-benchmark.md](../results/stage-2-posting-strategy-benchmark.md).
The setup was a GitHub-hosted runner with 4 vCPUs (AMD EPYC 9V74), Postgres 18.6
in a container on the same machine, and 16 workers on a 16-connection pool. Each
run did 4,000 transfers, and each cell is the median of 3 runs after a warm-up.
Accounts were funded generously, so the benchmark measures posting, not
refusing.

| Strategy | Scenario | Throughput (transfers/s) | p50 ms | p95 ms | p99 ms | Retries per run | Gave up per run |
|---|---|---:|---:|---:|---:|---:|---:|
| ORDERED_LOCKS | HOT (10 accounts) | 1351 | 6.05 | 44.89 | 80.67 | 0 | 0 |
| OPTIMISTIC | HOT | 888 | 4.88 | 101.35 | 175.32 | 6409 | 81 |
| SERIALIZABLE | HOT | 699 | 8.28 | 117.89 | 183.55 | 7378 | 96 |
| ORDERED_LOCKS | SPREAD (1,000 accounts) | 2143 | 7.31 | 11.63 | 14.58 | 0 | 0 |
| OPTIMISTIC | SPREAD | 1727 | 8.78 | 15.16 | 21.19 | 197 | 0 |
| SERIALIZABLE | SPREAD | 1177 | 9.79 | 35.47 | 58.43 | 1676 | 0 |

## Decision

**`ORDERED_LOCKS` is the ledger's posting strategy** (`ledger.posting.strategy`,
the default). The other two stay in the code base, selectable by configuration,
so the benchmark can be re-run as the system changes (Stage 10 revisits hot
accounts).

Why:

- **Highest throughput in both scenarios.** 1351 vs 888 vs 699 transfers/s on
  hot accounts; 2143 vs 1727 vs 1177 when spread out.
- **Best tail latency.** On hot accounts its p99 was 80.67 ms, against 175.32
  and 183.55 ms for the others.
- **No wasted work and no give-ups.** Contention costs only waiting time. Under
  contention the retrying strategies redid thousands of transactions per run,
  and some postings still failed after 10 attempts (a median of 81 and 96 out of
  each run's 4,000 on hot accounts). A failed posting returns 409, so the client has to retry. That is
  safe, but it is extra latency and noise.
- **Simple to reason about.** The lock order (account id) is the only rule, and
  the framework-free core (`PostingRules`) is shared by all three strategies.

`OPTIMISTIC` had the lowest median on hot accounts (4.88 ms vs 6.05 ms), because
an uncontended attempt doesn't wait for locks. That advantage disappears in the
tail and in throughput.

## Alternatives considered

The other two strategies were the alternatives; see Evidence. Beyond them:

- **A single conditional `UPDATE ... SET raw = raw + Δ WHERE (overdraft rule)`**
  per account, relying on the row lock the update takes. It is the same idea as
  ordered locks with fewer round trips. Worth trying in Stage 10, when hot
  accounts get special treatment anyway.
- **Advisory locks**: a lock in the database, but not tied to the rows being
  changed, so the protection relies on every code path remembering to take it.

## Consequences

- A very hot account (one merchant receiving every payment) serialises on its
  balance row. That is the bottleneck Stage 10's "hot-account" scenario will
  measure and fix (sub-account balance buckets, batching).
- Lock waits are bounded only by transaction length. Postings stay short: one
  read, one update per account and one batch insert, with no network calls
  inside the transaction.
- These numbers come from one CI runner with Postgres sharing the CPU. They
  rank the strategies; they don't predict production capacity. Stage 10
  measures the real deployment.
