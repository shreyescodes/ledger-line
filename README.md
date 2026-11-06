# Ledgerline

A double-entry payments ledger and payment-orchestration platform, built to be
provably correct under concurrency, retries and failures. Money, banks and the
PSP are simulated; this is not real UPI/NPCI.

**Status:** Stage 0, domain foundations (`v0.0`). No services yet.

## Start here

- [ledgerline.md](ledgerline.md): the project brief and stage plan.
- [DOMAIN.md](DOMAIN.md): the money model, covering accounts, holds, the
  payment saga, reconciliation, invariants and worked examples.
- [docs/adr/](docs/adr/README.md): architecture decision records.

## Verify Stage 0

Needs Java 21 or newer. There is no build step.

```sh
java tools/domain-check/DomainCheck.java DOMAIN.md
java tools/domain-check/DomainCheck.java tools/domain-check/testdata/broken.md
```

- The first command replays every worked example in `DOMAIN.md`. It must end
  with `RESULT: OK`.
- The second runs the checker on a file with 11 planted mistakes. It passes
  only if the checker catches all of them.

Recorded run: [docs/results/stage-0-domain-check.md](docs/results/stage-0-domain-check.md).

## Stages

| Stage | Scope | Status |
|---|---|---|
| 0 | Domain foundations: DOMAIN.md, first ADRs | done (`v0.0`) |
| 1 | Skeleton: monorepo, Gradle, Compose, Flyway, Testcontainers, ArchUnit, CI | next |
| 2 | Ledger core: Money, schema, atomic postings, three overdraft strategies benchmarked | |
| 3 | Idempotency, holds, state machine | |
| 4 | Events: transactional outbox, Kafka, inbox, DLQ | |
| 5 | Payments, mock PSP, saga, Resilience4j, webhooks | |
| 5b | UPI-style VPAs and collect requests (stretch) | |
| 6 | Reconciliation (Spring Batch) | |
| 7 | Security and multi-tenancy (OAuth2, RLS) | |
| 8 | Observability and SLOs | |
| 9 | Ops dashboard (React + TypeScript) | |
| 10 | Performance (k6, hot-account fix) | |
| 11 | AWS deployment (Terraform, ECS, RDS) | |
| 12 | Chaos experiments and postmortems | |
| 13 | Packaging: README, ADRs, runbooks, demo | |

## Measured results

None yet. Every performance or reliability number in this repository will link
to a saved run under `docs/results/`.

## Repository layout

The planned layout from the brief. Folders appear as the stages create them.

```text
services/    ledger-service, payment-service, mock-psp,
             reconciliation-service, notification-service
libs/common  Money, IDs, event envelope, idempotency utilities
web/         ops-dashboard
infra/       compose, terraform
load-tests/  k6 scenarios
chaos/       fault-injection experiments
docs/        adr, runbooks, results, postmortems, diagrams
tools/       domain-check (Stage 0)
```
