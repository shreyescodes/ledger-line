# Ledgerline

A double-entry payments ledger and payment-orchestration platform, built to be
provably correct under concurrency, retries and failures. Money, banks and the
PSP are simulated; this is not real UPI/NPCI.

**Status:** Stage 2, the ledger core (`v0.2`): double-entry postings with
database-enforced invariants, three overdraft-prevention strategies, statements
and a continuous invariant verifier. The other services are still skeletons.

## Start here

- [ledgerline.md](ledgerline.md): the project brief and stage plan.
- [DOMAIN.md](DOMAIN.md): the money model, covering accounts, holds, the
  payment saga, reconciliation, invariants and worked examples.
- [docs/adr/](docs/adr/README.md): architecture decision records.

## Run it locally

You need Docker (with Compose v2), Java 21 or newer to run Gradle (Gradle
downloads JDK 25 for the build itself), and `make`.

```sh
make up       # build the jars, start everything, wait until all containers are healthy
make smoke    # check the stack end to end
make down     # stop (make clean also deletes the database volume)
```

| What | Where |
|---|---|
| ledger-service, payment-service, mock-psp, reconciliation-service, notification-service | http://localhost:8081 to 8085 (`/actuator/health/readiness`, `/actuator/prometheus`) |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (credentials in `infra/compose/.env`) |
| Keycloak | http://localhost:8180 (realm `ledgerline`) |
| Postgres | `localhost:5432`, one database per service |
| Kafka | `localhost:19092` |
| Redis | `localhost:6379` |

`make up` copies `infra/compose/.env.example` to `infra/compose/.env` on first
run. Those are throwaway local credentials; real secrets never go in the repo.

## Ledger API (ledger-service, port 8081)

Every call names its tenant in an `X-Tenant-Id` header (Stage 7 replaces it with
a JWT). Amounts are integers in minor units, so `100000` is ₹1,000.00.

| Call | What it does |
|---|---|
| `POST /v1/accounts` | open an account: `{"type": "CUSTOMER_WALLET", "currency": "INR"}` |
| `GET /v1/accounts/{id}` | account with ledger and available balance |
| `GET /v1/accounts/{id}/entries?after=0&limit=50` | statement, keyset-paginated by `accountSeq` |
| `POST /v1/transactions` | post a balanced transaction (2 to 50 entries) |
| `POST /v1/transfers` | move value between two accounts of the same category |
| `GET /v1/invariants` | run the invariant checks on this tenant's books now |

Refusals come back as RFC 9457 problem responses (`insufficient-funds`,
`invalid-transaction`, `currency-mismatch`, `unknown-account`).

## Build and test

```sh
./gradlew build    # formatting, static analysis, unit, architecture and integration tests
make domain-check  # replay the DOMAIN.md worked examples
```

- Integration tests run against real Postgres through Testcontainers, so they
  need Docker. Without Docker they're skipped locally; in CI they always run.
- Formatting: `./gradlew spotlessApply` (CI uploads a ready-made patch when it fails).
- A failing property test prints a seed; replay it with
  `./gradlew test -Dproperty.seed=<seed> -Dproperty.tries=1`.
- Benchmark of the posting strategies (needs Docker):
  `./gradlew :services:ledger-service:benchmark`.
- After changing dependencies, refresh the lockfiles with `make lock`.

Windows note: if Gradle can't download from GitHub (connection resets over
IPv6), add `systemProp.java.net.preferIPv4Stack=true` and
`-Djava.net.preferIPv4Stack=true` (in `org.gradle.jvmargs`) to your user-level
`~/.gradle/gradle.properties`. `make` comes with Git for Windows' SDK, MSYS2 or
`choco install make`.

## Stages

| Stage | Scope | Status |
|---|---|---|
| 0 | Domain foundations: DOMAIN.md, first ADRs | done (`v0.0`) |
| 1 | Skeleton: monorepo, Gradle, Compose, Flyway, Testcontainers, ArchUnit, CI | done (`v0.1`) |
| 2 | Ledger core: Money, schema, atomic postings, three overdraft strategies benchmarked | done (`v0.2`) |
| 3 | Idempotency, holds, state machine | next |
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
to a saved run under `docs/results/`. Acceptance evidence so far:

- Stage 0: [domain-check run](docs/results/stage-0-domain-check.md)
- Stage 1: [CI run](docs/results/stage-1-ci.md)
- Stage 2: [CI run](docs/results/stage-2-ci.md) and
  [posting strategy benchmark](docs/results/stage-2-posting-strategy-benchmark.md)

## Repository layout

```text
build-logic/   shared Gradle convention plugins
libs/          common (plain Java, shared), test-support (Testcontainers, ArchUnit rules)
services/      ledger-service, payment-service, mock-psp,
               reconciliation-service, notification-service
infra/         compose (local stack), docker (service image)
docs/          adr, results (later: runbooks, postmortems, diagrams)
tools/         domain-check (Stage 0)
web/, load-tests/, chaos/, infra/terraform   arrive in later stages
```
