# Stage 1 acceptance run: CI on the Stage 1 pull request

Evidence for the Stage 1 criterion "CI is green on a PR with a Testcontainers
integration test". All excerpts below are copied from the raw job logs of the
run; nothing is edited except trimming unrelated lines. This is a correctness
run, not a performance measurement.

## Run

- Run: https://github.com/shreyescodes/ledger-line/actions/runs/42246368389
- Commit: `a40804eb9a623ae575220b693bed9d218f8ceafe`
- Started: 2026-11-06T15:47:49Z, finished: 2026-11-06T15:52:26Z (UTC)
- Result: **success**

| Job | Result |
|---|---|
| Dependency, secret and config scan | success |
| Build and test | success |
| Local stack smoke test | success |
| Image scan (payment-service) | success |
| Image scan (mock-psp) | success |
| Image scan (reconciliation-service) | success |
| Image scan (notification-service) | success |
| Image scan (ledger-service) | success |

## Environment

```text
Runner:
  Image: ubuntu-24.04
  Version: 20261101.360.1
  Image Release: https://github.com/actions/runner-images/releases/tag/ubuntu24%2F20261101.360
Java configuration:
  Distribution: temurin
  Version: 25.0.5+8
  Path: /opt/hostedtoolcache/Java_Temurin-Hotspot_jdk/25.0.5-8/x64
Welcome to Gradle 9.8.2!
```

## Build and test: every test result

```text
LedgerlineArchitectureRulesTest > commonLibIsFrameworkFree() PASSED
LedgerlineArchitectureRulesTest > onlyConfigurationReadsTheSystemClock() PASSED
LedgerlineArchitectureRulesTest > layersOnlyPointInwards() PASSED
LedgerlineArchitectureRulesTest > servicesCanNotUseEachOthersCode() PASSED
LedgerlineArchitectureRulesTest > domainCanNotUseSpringOrOuterLayers() PASSED
LedgerlineArchitectureRulesTest > domainHasNoFloatingPoint() PASSED
MockPspActuatorTest > livenessIsUp() PASSED
MockPspActuatorTest > prometheusMetricsAreTaggedWithTheApplicationName() PASSED
MockPspActuatorTest > readinessIsUpWithItsComponents() PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > SERVICES_ONLY_DEPEND_ON_THEMSELVES_AND_SHARED_LIBS PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > DOMAIN_IS_FRAMEWORK_FREE PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > LAYERS_POINT_INWARDS PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > NO_FLOATING_POINT_IN_THE_DOMAIN PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > TIME_COMES_FROM_AN_INJECTED_CLOCK PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > COMMON_IS_FRAMEWORK_FREE PASSED
NotificationActuatorTest > livenessIsUp() PASSED
NotificationActuatorTest > prometheusMetricsAreTaggedWithTheApplicationName() PASSED
NotificationActuatorTest > readinessIsUpWithItsComponents() PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > SERVICES_ONLY_DEPEND_ON_THEMSELVES_AND_SHARED_LIBS PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > DOMAIN_IS_FRAMEWORK_FREE PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > LAYERS_POINT_INWARDS PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > NO_FLOATING_POINT_IN_THE_DOMAIN PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > TIME_COMES_FROM_AN_INJECTED_CLOCK PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > COMMON_IS_FRAMEWORK_FREE PASSED
LedgerServiceIntegrationTest > livenessIsUp() PASSED
LedgerServiceIntegrationTest > prometheusMetricsAreTaggedWithTheApplicationName() PASSED
LedgerServiceIntegrationTest > readinessIsUpWithItsComponents() PASSED
LedgerServiceIntegrationTest > databaseDefaultsToUtc() PASSED
LedgerServiceIntegrationTest > flywayAppliedTheBaselineMigration() PASSED
ReconciliationActuatorTest > livenessIsUp() PASSED
ReconciliationActuatorTest > prometheusMetricsAreTaggedWithTheApplicationName() PASSED
ReconciliationActuatorTest > readinessIsUpWithItsComponents() PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > SERVICES_ONLY_DEPEND_ON_THEMSELVES_AND_SHARED_LIBS PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > DOMAIN_IS_FRAMEWORK_FREE PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > LAYERS_POINT_INWARDS PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > NO_FLOATING_POINT_IN_THE_DOMAIN PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > TIME_COMES_FROM_AN_INJECTED_CLOCK PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > COMMON_IS_FRAMEWORK_FREE PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > SERVICES_ONLY_DEPEND_ON_THEMSELVES_AND_SHARED_LIBS PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > DOMAIN_IS_FRAMEWORK_FREE PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > LAYERS_POINT_INWARDS PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > NO_FLOATING_POINT_IN_THE_DOMAIN PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > TIME_COMES_FROM_AN_INJECTED_CLOCK PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > COMMON_IS_FRAMEWORK_FREE PASSED
PaymentServiceIntegrationTest > livenessIsUp() PASSED
PaymentServiceIntegrationTest > prometheusMetricsAreTaggedWithTheApplicationName() PASSED
PaymentServiceIntegrationTest > readinessIsUpWithItsComponents() PASSED
PaymentServiceIntegrationTest > flywayAppliedTheBaselineMigration() PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > SERVICES_ONLY_DEPEND_ON_THEMSELVES_AND_SHARED_LIBS PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > DOMAIN_IS_FRAMEWORK_FREE PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > LAYERS_POINT_INWARDS PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > NO_FLOATING_POINT_IN_THE_DOMAIN PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > TIME_COMES_FROM_AN_INJECTED_CLOCK PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > COMMON_IS_FRAMEWORK_FREE PASSED
BUILD SUCCESSFUL in 54s
```

## Testcontainers: real Postgres behind the integration tests

From the JUnit XML reports in the run's `test-reports` artifact. `skipped="0"`
shows the Docker tests really ran (in CI they are never skipped, see ADR-0010).

```text
name="dev.ledgerline.ledger.LedgerServiceIntegrationTest" tests="5" skipped="0" failures="0" errors="0"
tc.postgres:18.6-alpine                  : Creating container for image: postgres:18.6-alpine
tc.postgres:18.6-alpine                  : Container postgres:18.6-alpine started in PT2.36283581S
org.flywaydb.core.FlywayExecutor         : Database: jdbc:postgresql://localhost:32770/test?loggerLevel=OFF (PostgreSQL 18.6)
o.f.core.internal.command.DbMigrate      : Current version of schema "public": << Empty Schema >>
o.f.core.internal.command.DbMigrate      : Migrating schema "public" to version "1 - baseline"
o.f.core.internal.command.DbMigrate      : Successfully applied 1 migration to schema "public", now at version v1 (execution time 00:00.011s)

name="dev.ledgerline.payment.PaymentServiceIntegrationTest" tests="4" skipped="0" failures="0" errors="0"
tc.postgres:18.6-alpine                  : Creating container for image: postgres:18.6-alpine
tc.postgres:18.6-alpine                  : Container postgres:18.6-alpine started in PT2.024037676S
org.flywaydb.core.FlywayExecutor         : Database: jdbc:postgresql://localhost:32771/test?loggerLevel=OFF (PostgreSQL 18.6)
o.f.core.internal.command.DbMigrate      : Current version of schema "public": << Empty Schema >>
o.f.core.internal.command.DbMigrate      : Migrating schema "public" to version "1 - baseline"
o.f.core.internal.command.DbMigrate      : Successfully applied 1 migration to schema "public", now at version v1 (execution time 00:00.005s)

```

Totals across every JUnit report in the run:

```text
11 test classes, 54 tests, 0 skipped, 0 failures, 0 errors
```

## Dependency, secret and config scan (Trivy filesystem)

```text
2026-11-06T15:48:05Z	INFO	Number of language-specific files	num=7
2026-11-06T15:48:05Z	INFO	Detected config files	num=1
Report Summary

┌─────────────────────────────────────────────────┬────────────┬─────────────────┬─────────┬───────────────────┐
│                     Target                      │    Type    │ Vulnerabilities │ Secrets │ Misconfigurations │
├─────────────────────────────────────────────────┼────────────┼─────────────────┼─────────┼───────────────────┤
│ libs/common/gradle.lockfile                     │   gradle   │        0        │    -    │         -         │
├─────────────────────────────────────────────────┼────────────┼─────────────────┼─────────┼───────────────────┤
│ libs/test-support/gradle.lockfile               │   gradle   │        0        │    -    │         -         │
├─────────────────────────────────────────────────┼────────────┼─────────────────┼─────────┼───────────────────┤
│ services/ledger-service/gradle.lockfile         │   gradle   │        0        │    -    │         -         │
├─────────────────────────────────────────────────┼────────────┼─────────────────┼─────────┼───────────────────┤
│ services/mock-psp/gradle.lockfile               │   gradle   │        0        │    -    │         -         │
├─────────────────────────────────────────────────┼────────────┼─────────────────┼─────────┼───────────────────┤
│ services/notification-service/gradle.lockfile   │   gradle   │        0        │    -    │         -         │
├─────────────────────────────────────────────────┼────────────┼─────────────────┼─────────┼───────────────────┤
│ services/payment-service/gradle.lockfile        │   gradle   │        0        │    -    │         -         │
├─────────────────────────────────────────────────┼────────────┼─────────────────┼─────────┼───────────────────┤
│ services/reconciliation-service/gradle.lockfile │   gradle   │        0        │    -    │         -         │
├─────────────────────────────────────────────────┼────────────┼─────────────────┼─────────┼───────────────────┤
│ infra/docker/service.Dockerfile                 │ dockerfile │        -        │    -    │         0         │
└─────────────────────────────────────────────────┴────────────┴─────────────────┴─────────┴───────────────────┘
Legend:
```

## Image scan (ledger-service; the other four images use the same base)

```text
Report Summary

┌─────────────────────────────────────────────┬────────┬─────────────────┬─────────┐
│                   Target                    │  Type  │ Vulnerabilities │ Secrets │
├─────────────────────────────────────────────┼────────┼─────────────────┼─────────┤
│ ledgerline/ledger-service:ci (ubuntu 24.04) │ ubuntu │        0        │    -    │
├─────────────────────────────────────────────┼────────┼─────────────────┼─────────┤
│ app/app.jar                                 │  jar   │        0        │    -    │
└─────────────────────────────────────────────┴────────┴─────────────────┴─────────┘
Legend:
```

## Local stack smoke test

```text
ok    postgres has one database per service: ledger,notification,payment,postgres,reconciliation,template0,template1
ok    kafka broker answers
ok    redis answers PONG
ok    ledger-service ready: {"components":{"db":{"status":"UP"},"readinessState":{"status":"UP"}},"status":"UP"}
ok    payment-service ready: {"components":{"db":{"status":"UP"},"readinessState":{"status":"UP"}},"status":"UP"}
ok    mock-psp ready: {"components":{"readinessState":{"status":"UP"}},"status":"UP"}
ok    reconciliation-service ready: {"components":{"readinessState":{"status":"UP"}},"status":"UP"}
ok    notification-service ready: {"components":{"readinessState":{"status":"UP"}},"status":"UP"}
ok    prometheus scrapes all 5 services
ok    grafana reaches prometheus
ok    keycloak serves the ledgerline realm
SMOKE TEST PASSED
```
