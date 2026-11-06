# ADR-0010: CI pipeline and supply-chain checks

- **Status:** Proposed
- **Date:** 2026-11-06
- **Stage:** 1

## Context

The brief asks for GitHub Actions with build, test, dependency scanning and
Trivy, plus formatter and static analysis in CI. Stage 1 is done when CI is
green on a PR that includes a Testcontainers integration test. Docker isn't
installed on the development machine yet, so CI is also where the Docker-based
tests and the Compose stack are proven.

## Decision

One workflow, `.github/workflows/ci.yml`, runs on every pull request and on
pushes to `main`, with these jobs:

| Job | What it proves |
|---|---|
| Build and test | DOMAIN.md examples replay; formatting (Spotless), static analysis (Error Prone, `-Werror`), unit tests, ArchUnit rules, and the Testcontainers integration tests all pass |
| Dependency, secret and config scan | Trivy finds no HIGH/CRITICAL vulnerabilities that have a fix, reading exact versions from `gradle.lockfile`; no committed secrets; no Dockerfile misconfigurations |
| Image scan (×5) | each service image has no HIGH/CRITICAL vulnerabilities that have a fix |
| Local stack smoke test | `docker compose up --wait` brings every container to healthy, and `smoke-test.sh` checks the databases, Kafka, Redis, service readiness, Prometheus scraping, Grafana → Prometheus and the Keycloak realm |

Rules:

1. **Docker tests can't be skipped in CI.** Tests marked `@RequiresDocker` skip
   on a machine without Docker, but run (and fail loudly) when `CI` is set.
2. **Third-party actions are pinned to full commit SHAs**, with the release tag
   in a comment. Tags can be moved; commits can't. This is not theoretical.
   In March 2026, attackers force-pushed malicious code to 76 of the 77 tags
   of `aquasecurity/trivy-action` (CVE-2026-33634), and every workflow that
   referenced those tags ran it. We pin trivy-action v0.36.0, released after
   the cleanup. It installs the Trivy binary through `setup-trivy`, itself
   pinned by SHA to v0.2.6. The binary version (v0.70.0) is also set
   explicitly in our workflow.
3. **Least-privilege token:** `permissions: contents: read`.
4. **Scan threshold:** fail on HIGH and CRITICAL findings that have a fix.
   Unfixed findings are reported but don't block, because nothing we do can
   fix them yet.
5. Jars are built once and shared between jobs as an artifact.

## Alternatives considered

- **OWASP Dependency-Check**: thorough, but slow, and it needs an NVD API key
  in CI. Trivy covers dependencies, images, secrets and IaC with one tool,
  which the brief already lists.
- **GitHub dependency review**: needs the Gradle dependency graph submitted on
  every run. Possible later; Trivy already blocks known-vulnerable versions.
- **Dependabot / Renovate for automatic update PRs**: useful, but it's a new
  service acting on the repo, so per the brief we ask first. It is suggested
  in the Stage 1 PR.
- **Tags instead of SHAs for actions**: easier to read and update, but
  mutable.

## Consequences

- A new HIGH/CRITICAL CVE in a dependency can turn `main` red overnight. That's
  the point; the fix is a version bump or a documented exception.
- Pinned SHAs need periodic bumps (a tool like Renovate would automate this).
- The smoke test makes CI slower (several minutes to pull images), but it is
  the only automated proof that `make up` works.
