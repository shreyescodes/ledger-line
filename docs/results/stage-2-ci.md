# Stage 2 acceptance run: CI on the Stage 2 pull request

Evidence for the Stage 2 criterion "1,000 parallel transfers across 10 accounts
leave every invariant intact", plus the property and database-enforcement tests.
Excerpts are copied from the raw job logs and JUnit reports of the run, trimmed
but not edited. This is a correctness run, not a performance measurement; the
benchmark has its own results file.

## Run

- Run: https://github.com/shreyescodes/ledger-line/actions/runs/42468912225
- Commit: `4d5573c8302c811726a2d5afdf28ee1d7a0ba858`
- Started: 2026-11-08T09:51:05Z, finished: 2026-11-08T09:55:32Z (UTC)
- Result: **success**

| Job | Result |
|---|---|
| Build and test | success |
| Dependency, secret and config scan | success |
| Local stack smoke test | success |
| Image scan (reconciliation-service) | success |
| Image scan (payment-service) | success |
| Image scan (notification-service) | success |
| Image scan (mock-psp) | success |
| Image scan (ledger-service) | success |
| Posting strategy benchmark | skipped |

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
```

## Build and test: every test result

```text
TimeOrderedIdGeneratorTest > makesVersion7IdsStampedWithTheInjectedClock() PASSED
TimeOrderedIdGeneratorTest > idsFromTheSameMillisecondStillSortInCreationOrder() PASSED
BasisPointsTest > roundsHalfUpExactlyOnce() PASSED
BasisPointsTest > theWorkedExampleFromDomainMd() PASSED
BasisPointsTest > refusesNegativeAmountsAndImpossibleRates() PASSED
BasisPointsTest > feePlusNetIsAlwaysTheWholeAmount() PASSED
MoneyTest > addsAndSubtractsWithinOneCurrency() PASSED
MoneyTest > currencyCodesMustBeRealIso4217Codes() PASSED
MoneyTest > overflowFailsInsteadOfWrappingAround() PASSED
MoneyTest > formatsWithTheCurrencysOwnNumberOfDecimals() PASSED
MoneyTest > refusesCrossCurrencyArithmetic() PASSED
LedgerlineArchitectureRulesTest > commonLibIsFrameworkFree() PASSED
LedgerlineArchitectureRulesTest > onlyConfigurationReadsTheSystemClock() PASSED
LedgerlineArchitectureRulesTest > layersOnlyPointInwards() PASSED
LedgerlineArchitectureRulesTest > servicesCanNotUseEachOthersCode() PASSED
LedgerlineArchitectureRulesTest > domainCanNotUseSpringOrOuterLayers() PASSED
LedgerlineArchitectureRulesTest > domainHasNoFloatingPoint() PASSED
PropertyCheckTest > shrinksAFailureToTheSmallestCounterexample() PASSED
PropertyCheckTest > theReportedSeedReproducesTheFailingValue() PASSED
PropertyCheckTest > runsEveryTryWhenThePropertyHolds() PASSED
MockPspActuatorTest > livenessIsUp() PASSED
NotificationActuatorTest > livenessIsUp() PASSED
NotificationActuatorTest > prometheusMetricsAreTaggedWithTheApplicationName() PASSED
NotificationActuatorTest > readinessIsUpWithItsComponents() PASSED
MockPspActuatorTest > prometheusMetricsAreTaggedWithTheApplicationName() PASSED
MockPspActuatorTest > readinessIsUpWithItsComponents() PASSED
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
ReconciliationActuatorTest > livenessIsUp() PASSED
ReconciliationActuatorTest > prometheusMetricsAreTaggedWithTheApplicationName() PASSED
ReconciliationActuatorTest > readinessIsUpWithItsComponents() PASSED
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
ConcurrentTransfersIntegrationTest > thousandParallelTransfersAcrossTenAccountsKeepEveryInvariant(PostingStrategyName) > ORDERED_LOCKS PASSED
ConcurrentTransfersIntegrationTest > thousandParallelTransfersAcrossTenAccountsKeepEveryInvariant(PostingStrategyName) > OPTIMISTIC PASSED
ConcurrentTransfersIntegrationTest > thousandParallelTransfersAcrossTenAccountsKeepEveryInvariant(PostingStrategyName) > SERIALIZABLE PASSED
LedgerApiIntegrationTest > anOverdraftIsRefusedAndChangesNothing() PASSED
LedgerApiIntegrationTest > statementsPageThroughEveryEntryExactlyOnce() PASSED
LedgerApiIntegrationTest > badRequestsAreRejectedBeforeTheLedger() PASSED
LedgerApiIntegrationTest > openAccountReturnsItWithAZeroBalance() PASSED
LedgerApiIntegrationTest > transferMovesMoneyAndShowsUpOnBothStatements() PASSED
LedgerApiIntegrationTest > anUnbalancedTransactionIsRefused() PASSED
LedgerApiIntegrationTest > theInvariantEndpointReportsHealthyBooks() PASSED
LedgerApiIntegrationTest > anotherTenantCanNotSeeOrUseTheseAccounts() PASSED
LedgerPropertiesIntegrationTest > anySequenceOfPostingsKeepsTheStoredBooksBalanced() PASSED
LedgerSchemaIntegrationTest > anUnbalancedTransactionIsRefusedAtCommit() PASSED
LedgerSchemaIntegrationTest > theAppRoleCanNotUpdateOrDeleteEntries() PASSED
LedgerSchemaIntegrationTest > theAppRoleCanNotChangeAnAccountsFlags() PASSED
LedgerSchemaIntegrationTest > theAppRoleCanStillPostThroughTheLedger() PASSED
LedgerSchemaIntegrationTest > anEntryMustBeInItsAccountsCurrency() PASSED
LedgerSchemaIntegrationTest > evenTheOwnerCanNotRewriteHistory() PASSED
LedgerSchemaIntegrationTest > aTransactionNeedsAtLeastTwoEntries() PASSED
LedgerSchemaIntegrationTest > theDatabaseItselfRefusesAnOverdraft() PASSED
LedgerServiceIntegrationTest > livenessIsUp() PASSED
LedgerServiceIntegrationTest > prometheusMetricsAreTaggedWithTheApplicationName() PASSED
LedgerServiceIntegrationTest > readinessIsUpWithItsComponents() PASSED
LedgerServiceIntegrationTest > invariantMetricsAreExported() PASSED
LedgerServiceIntegrationTest > theServiceConnectsAsTheLeastPrivilegeRole() PASSED
LedgerServiceIntegrationTest > databaseDefaultsToUtc() PASSED
LedgerServiceIntegrationTest > flywayAppliedEveryMigration() PASSED
PostingRulesPropertyTest > anySequenceOfPostingsKeepsTheBooksBalanced() PASSED
PostingRulesTest > aMerchantPaymentWithFeeSplitsTheMoneyAndBumpsVersions() PASSED
PostingRulesTest > entriesMustBeInTheAccountsCurrencyAndInThisTenantsBooks() PASSED
PostingRulesTest > aWalletCanNotGoBelowZero() PASSED
PostingRulesTest > aMerchantMayGoNegativeThroughAForcedDebit() PASSED
PostingRulesTest > draftsMustHaveTwoBalancedEntriesWithPositiveAmounts() PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > SERVICES_ONLY_DEPEND_ON_THEMSELVES_AND_SHARED_LIBS PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > DOMAIN_IS_FRAMEWORK_FREE PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > LAYERS_POINT_INWARDS PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > NO_FLOATING_POINT_IN_THE_DOMAIN PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > TIME_COMES_FROM_AN_INJECTED_CLOCK PASSED
ArchitectureTest > LedgerlineArchitectureRules > LedgerlineArchitectureRules > COMMON_IS_FRAMEWORK_FREE PASSED
BUILD SUCCESSFUL in 1m 6s
```

## Acceptance: 1,000 parallel transfers across 10 accounts, per strategy

From ConcurrentTransfersIntegrationTest's report. Each line is one strategy; after each,
the test asserts zero invariant violations, no negative wallet, money conserved across the
wallets, and exactly two entries per posted transfer.

```text
CONCURRENCY ORDERED_LOCKS: 1000 transfers, 892 posted, 108 rejected (insufficient funds), 0 gave up, 0 retries
CONCURRENCY OPTIMISTIC: 1000 transfers, 859 posted, 113 rejected (insufficient funds), 28 gave up, 2048 retries
CONCURRENCY SERIALIZABLE: 1000 transfers, 856 posted, 129 rejected (insufficient funds), 15 gave up, 1996 retries
name="dev.ledgerline.ledger.ConcurrentTransfersIntegrationTest" tests="3" skipped="0" failures="0" errors="0"
name="dev.ledgerline.ledger.LedgerApiIntegrationTest" tests="8" skipped="0" failures="0" errors="0"
name="dev.ledgerline.ledger.LedgerPropertiesIntegrationTest" tests="1" skipped="0" failures="0" errors="0"
name="dev.ledgerline.ledger.LedgerSchemaIntegrationTest" tests="8" skipped="0" failures="0" errors="0"
name="dev.ledgerline.ledger.PostingRulesPropertyTest" tests="1" skipped="0" failures="0" errors="0"
```

## Testcontainers: real Postgres behind the integration tests

From the JUnit XML reports in the run's `test-reports` artifact. `skipped="0"`
shows the Docker tests really ran (in CI they are never skipped, see ADR-0010).

```text
name="dev.ledgerline.ledger.ConcurrentTransfersIntegrationTest" tests="3" skipped="0" failures="0" errors="0"
tc.postgres:18.6-alpine                  : Creating container for image: postgres:18.6-alpine
tc.postgres:18.6-alpine                  : Container postgres:18.6-alpine started in PT1.979040438S
org.flywaydb.core.FlywayExecutor         : Database: jdbc:postgresql://localhost:32770/test?loggerLevel=OFF (PostgreSQL 18.6)
o.f.core.internal.command.DbMigrate      : Current version of schema "public": << Empty Schema >>
o.f.core.internal.command.DbMigrate      : Migrating schema "public" to version "1 - baseline"
o.f.core.internal.command.DbMigrate      : Migrating schema "public" to version "2 - ledger tables"
o.f.core.internal.command.DbMigrate      : Migrating schema "public" to version "3 - ledger invariants"
o.f.core.internal.command.DbMigrate      : Migrating schema "public" to version "4 - app role privileges"
o.f.core.internal.command.DbMigrate      : Successfully applied 4 migrations to schema "public", now at version v4 (execution time 00:00.053s)

name="dev.ledgerline.ledger.LedgerApiIntegrationTest" tests="8" skipped="0" failures="0" errors="0"

name="dev.ledgerline.ledger.LedgerPropertiesIntegrationTest" tests="1" skipped="0" failures="0" errors="0"

name="dev.ledgerline.ledger.LedgerSchemaIntegrationTest" tests="8" skipped="0" failures="0" errors="0"

name="dev.ledgerline.ledger.LedgerServiceIntegrationTest" tests="7" skipped="0" failures="0" errors="0"

name="dev.ledgerline.payment.PaymentServiceIntegrationTest" tests="4" skipped="0" failures="0" errors="0"
tc.postgres:18.6-alpine                  : Creating container for image: postgres:18.6-alpine
tc.postgres:18.6-alpine                  : Container postgres:18.6-alpine started in PT2.267973178S
org.flywaydb.core.FlywayExecutor         : Database: jdbc:postgresql://localhost:32771/test?loggerLevel=OFF (PostgreSQL 18.6)
o.f.core.internal.command.DbMigrate      : Current version of schema "public": << Empty Schema >>
o.f.core.internal.command.DbMigrate      : Migrating schema "public" to version "1 - baseline"
o.f.core.internal.command.DbMigrate      : Successfully applied 1 migration to schema "public", now at version v1 (execution time 00:00.012s)

```

Totals across every JUnit report in the run:

```text
21 test classes, 96 tests, 0 skipped, 0 failures, 0 errors
```

## Dependency, secret and config scan (Trivy filesystem)

```text
2026-11-08T09:51:20Z	INFO	Number of language-specific files	num=7
2026-11-08T09:51:20Z	INFO	Detected config files	num=1
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
