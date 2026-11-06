# LEDGERLINE — double-entry payments ledger & orchestration platform
(Working name. Simulated money, simulated PSP/bank. Not real UPI/NPCI.)

## 0. HOW WE WORK (read first, follow always)
- You are a senior backend engineer pair-building with me. I'm strong in Python/FastAPI, Postgres and React, but NEW to Java and Spring Boot. Teach as you build: before each stage give a short plan (files + concepts); after it, a recap (what was built, which Java/Spring idioms you used and why, what could go wrong).
- Work ONE stage at a time. Stop at the end of each stage and wait for me to say "continue".
- A stage is done only when its acceptance criteria are proven by automated tests or commands whose output you show me. Never say "should work".
- Small commits (conventional commits). Write an ADR in docs/adr for every real design decision. Update README/docs and tag each stage (v0.N).
- NEVER invent measurements. Every performance/reliability number in any doc must come from a run saved under docs/results/ with the raw output and environment description.
- Pin the latest stable versions of Java (current LTS, 21+), Spring Boot and all libraries by checking current official docs, not memory.
- Ask before adding any major dependency or service not listed here.

## 1. GOAL & NON-GOALS
Goal: a production-style ledger + payment orchestration platform that is provably correct under concurrency, retries and failures, observable, deployed on AWS, and well documented.
Non-goals: real money, real bank/NPCI integration, KYC, card data/PCI.

## 2. STACK
Backend: Java (current LTS), Spring Boot, Gradle (Kotlin DSL) multi-module monorepo. Postgres + Flyway. Spring Data JDBC or jOOQ on the ledger hot path (explicit SQL and locking; decide in an ADR), JPA allowed elsewhere. Kafka (KRaft) + Spring Kafka. Redis (Bucket4j rate limiting; optional idempotency fast-path). Resilience4j. Spring Security OAuth2 resource server (Keycloak locally). Spring Batch (reconciliation). Micrometer + Prometheus + Grafana + OpenTelemetry. Tests: JUnit 5, AssertJ, Testcontainers, jqwik (property-based), ArchUnit. Load: k6. Faults: Toxiproxy. IaC: Terraform. CI/CD: GitHub Actions. Local: Docker Compose + Makefile.
Frontend: React + TypeScript (strict, no `any`) + Vite, TanStack Query, React Router, Recharts, typed client generated from OpenAPI (openapi-typescript), Vitest + Testing Library, Playwright smoke tests, ESLint + Prettier.

## 3. REPO LAYOUT
services/ledger-service, payment-service, mock-psp, reconciliation-service, notification-service
libs/common (Money, IDs, event envelope, idempotency utilities)
web/ops-dashboard
infra/compose, infra/terraform
load-tests/, chaos/
docs/adr, runbooks, results, postmortems, diagrams (Mermaid/C4)

## 4. NON-NEGOTIABLE INVARIANTS
1. Money = long minor units + ISO currency code. No floating point. No cross-currency arithmetic.
2. Every transaction has 2+ entries; per currency, debits == credits. Enforced in code AND in the DB (deferred constraint trigger).
3. Entries are immutable: no UPDATE/DELETE (app DB role lacks the privilege + trigger). Corrections are reversing transactions.
4. Balance == sum of entries. A stored balance row is a cached projection updated in the SAME DB transaction as the entries; a verifier job proves equality continuously.
5. Accounts flagged non-negative can never overdraft, even under concurrency.
6. Same Idempotency-Key + same payload -> same response, effect exactly once. Same key + different payload -> 422. Concurrent duplicates -> one executes, others wait or get 409.
7. Postgres is authoritative for idempotency; Redis (if used) is only a cache.
8. Events go out via the transactional outbox. Delivery is at-least-once, consumers are idempotent (inbox/dedup table) -> effectively-once effects. Never claim "exactly-once".
9. Tenant isolation: tenant_id on every table + Postgres row-level security.
10. Time-ordered IDs (UUIDv7/ULID via a library), UTC `Instant`, injected `Clock`.
11. No PII in ledger tables; use opaque references.

## 5. SERVICES
- ledger-service: accounts (incl. system accounts: suspense, fees, settlement, PSP-clearing), transfers, holds (authorize/capture/void), ledger vs available balance, statements (keyset pagination), outbox relay, invariant verifier job.
- payment-service: public API (create/get payment, refund), Idempotency-Key handling, per-tenant token-bucket rate limiting, a RiskCheck port (stub allow-all for now; a fraud-scoring service can plug in later by consuming ledger events), persisted saga orchestrator, PSP adapter with Resilience4j (circuit breaker, retry with exponential backoff + jitter, time limiter, bulkhead), PSP webhook receiver (HMAC signature, timestamp tolerance, replay protection), outbox.
- mock-psp: simulated bank/PSP with a control API for failure modes (latency, 5xx, timeouts, duplicate and out-of-order webhooks, partial outage) and a daily settlement-file generator (CSV) that can seed discrepancies on demand.
- reconciliation-service: Spring Batch job: import settlement file, match to ledger, emit break reason codes AMOUNT_MISMATCH, MISSING_IN_PSP, MISSING_IN_LEDGER, TIMING_DIFF, DUPLICATE_IN_PSP, CURRENCY_MISMATCH. Park unresolved amounts in the suspense account via adjustment transactions. Ops resolve workflow with audit trail.
- notification-service: consumes events, sends signed outbound webhooks to merchants with retry/backoff/DLQ, idempotent handlers.

Saga (persisted, every transition stored): INITIATED -> FUNDS_HELD -> PSP_SUBMITTED -> PSP_CONFIRMED -> POSTED -> COMPLETED. Failure: COMPENSATING -> COMPENSATED / FAILED. A PSP timeout is NOT a failure: go to PSP_UNKNOWN, poll the PSP status endpoint with backoff, and hand off to reconciliation if still unresolved. Never compensate blindly.
Circuit breaker OPEN: accept payment as PENDING_PSP (queued) and retry when the breaker half-opens; the API must return a clear status.

## 6. STAGES (do one at a time; each ends with demonstrated acceptance criteria)
Stage 0 — Domain foundations (no code): DOMAIN.md with account types, glossary, invariants and worked examples (wallet top-up, P2P transfer, merchant payment with fee, refund, chargeback, PSP-timeout-unknown). Done when each example balances on paper.
Stage 1 — Skeleton: monorepo, Gradle, Compose (Postgres, Kafka, Redis, Keycloak, Prometheus, Grafana), Flyway, Testcontainers base, ArchUnit module-boundary rules, GitHub Actions (build, test, dependency scan, Trivy), health endpoints. Done when CI is green on a PR with a Testcontainers integration test.
Stage 2 — Ledger core: Money, schema, atomic transfer posting, statements, balance. Implement THREE overdraft-prevention strategies (ordered row locks, optimistic version + retry, serializable + retry), benchmark them, record the winner in an ADR. jqwik property tests (sum of all balances == 0, no negative balances, replay of entries == balances). Done when 1,000 parallel transfers across 10 accounts leave every invariant intact.
Stage 3 — Idempotency, holds, state machine: authoritative idempotency table (key, request hash, stored response, status, TTL cleanup), hold/capture/void, ledger vs available balance. Done when retrying a request 50x concurrently yields exactly one effect.
Stage 4 — Events: transactional outbox -> Kafka (key = account/payment id), versioned event schemas with compatibility checks, inbox-based idempotent consumers, retry topics + DLQ. Done when killing the relay mid-flight loses and duplicates nothing (automated test). Stretch: Debezium CDC in place of the poller.
Stage 5 — Payments + PSP + saga: payment-service, mock-psp, saga, Resilience4j config, webhook verification, unknown-outcome handling. Done when every injected PSP failure mode ends in a consistent ledger and each breaker state transition is covered by a test.
Stage 5b (stretch, UPI-style flavour): VPA aliases, collect requests (REQUESTED -> APPROVED/DECLINED/EXPIRED) with expiry.
Stage 6 — Reconciliation: Spring Batch job, reason codes, suspense adjustments, resolve workflow. Done when every seeded mismatch is detected, categorized correctly and reported.
Stage 7 — Security & multi-tenancy: OAuth2/JWT scopes, RLS, least-privilege DB roles, secrets handling, audit log, rate-limit tests. Done when a test proves tenant A can never read tenant B's data even through a deliberately buggy query.
Stage 8 — Observability & SLOs: JSON logs with traceId/paymentId/tenantId, OpenTelemetry traces across HTTP -> Kafka -> consumer, Micrometer metrics (RED + payments by state, saga duration, outbox lag, consumer lag, breaker state, reconciliation breaks, invariant violations), Grafana dashboards as code, SLOs with burn-rate alerts, a runbook per alert. Done when an injected fault can be diagnosed from dashboards alone.
Stage 9 — Ops dashboard (React + TypeScript): OIDC login (PKCE), typed OpenAPI client, live payment feed via SSE, payment detail with saga timeline + ledger entries, ledger explorer (ledger vs available balance), reconciliation breaks (filter/resolve), resilience panel (breaker state per PSP, retry queue depth, consumer lag), invariant monitor (sum == 0, last verified), idempotency tester (fire N duplicates, show exactly one effect), chaos panel (toggle mock-psp failure modes; demo environments only, guarded). Vitest + Testing Library for components, Playwright smoke test for the demo flow.
Stage 10 — Performance: k6 scenarios (steady load; 10% duplicate requests; hot-account where all payments hit one merchant; slow PSP). Record p50/p95/p99, throughput, error rate, DB CPU. Fix the hot-account problem (sub-account balance buckets, batching, pool tuning, indexes, entries partitioning) and record before/after in docs/results. Done when every claim in the README links to a saved run.
Stage 11 — AWS deployment: Terraform (VPC with private subnets, ECS Fargate, RDS Postgres, ALB, ECR, Secrets Manager, CloudWatch, S3 + CloudFront for the dashboard, Kafka hosting chosen in an ADR with the cost trade-off). GitHub Actions deploy via OIDC (no long-lived keys), rolling or blue/green with automatic rollback on failed health checks, expand/contract Flyway migrations. docs/cost.md (verify against current AWS pricing) and a one-command teardown. Done when a deliberately bad build is deployed and rolls back automatically.
Stage 12 — Chaos & postmortems: experiments with hypothesis -> method -> observed -> fix: DB restart/failover, Kafka broker down, Toxiproxy latency/partition, poison message, duplicate/out-of-order events, PSP timeout storm, relay crash. Publish 2 blameless postmortems.
Stage 13 — Packaging: README (C4 diagram, measured results table, one-command local run, demo link/video), 10+ ADRs (money representation, locking strategy, idempotency store, outbox vs CDC, event versioning, orchestration vs choreography, unknown-outcome handling, RLS multi-tenancy, ID scheme, Kafka hosting/cost), runbooks, OpenAPI docs, docs/interview-notes.md (system-design talking points: cost, failure modes, monitoring, rollback), a 3-minute demo script (happy path -> kill the PSP -> watch the saga/breaker behave -> run reconciliation).

## 7. QUALITY BAR
- Tests: unit, Testcontainers integration, property-based, concurrency, ArchUnit, event-schema compatibility, e2e smoke. Coverage gate on domain code.
- Formatter + static analysis in CI. Dependency and image scanning in CI.
- Security: input validation, no secrets in the repo, least-privilege DB roles, OWASP API Top 10 checklist in docs.
- Every service exposes health, readiness and metrics.

## 8. DEFINITION OF DONE
Fresh clone -> `make up` runs everything locally; CI green; every acceptance criterion automated; all published numbers measured and linked; AWS demo deployable and tear-down-able; docs complete.

## 9. OUT OF SCOPE (later)
A separate fraud-scoring service that consumes the ledger's event stream and plugs into the RiskCheck port.