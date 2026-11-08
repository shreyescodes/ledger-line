# Architecture decision records

How we record decisions: [ADR-0001](0001-record-architecture-decisions.md).
New ADRs start from [template.md](template.md).

| ADR | Decision | Status | Stage |
|---|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions as ADRs | Accepted | 0 |
| [0002](0002-money-as-integer-minor-units.md) | Represent money as integer minor units | Accepted | 0 |
| [0003](0003-chart-of-accounts-and-sign-convention.md) | Chart of accounts and debit/credit sign convention | Accepted | 0 |
| [0004](0004-holds-as-reservations.md) | Model holds as reservations, not ledger entries | Accepted | 0 |
| [0005](0005-payment-flow-model.md) | Route every customer and merchant payment through the PSP | Accepted | 0 |
| [0006](0006-psp-timeouts-are-unknown-outcomes.md) | Treat PSP timeouts as unknown outcomes | Accepted | 0 |
| [0007](0007-gradle-build-and-dependency-management.md) | Gradle multi-module build with convention plugins and locked dependencies | Accepted | 1 |
| [0008](0008-service-structure-and-architecture-rules.md) | Hexagonal service structure enforced by ArchUnit | Accepted | 1 |
| [0009](0009-database-per-service.md) | One database per service, migrated by its owner | Accepted | 1 |
| [0010](0010-ci-pipeline-and-supply-chain.md) | CI pipeline and supply-chain checks | Accepted | 1 |
| [0011](0011-time-ordered-uuidv7-ids.md) | Time-ordered UUIDv7 ids, made by the application | Proposed | 2 |
| [0012](0012-plain-sql-with-jdbcclient-on-the-hot-path.md) | Plain SQL with Spring's JdbcClient for the ledger | Proposed | 2 |
| [0013](0013-ledger-schema-and-database-enforced-invariants.md) | Ledger schema, and invariants the database enforces itself | Proposed | 2 |
| [0014](0014-ordered-row-locks-prevent-overdrafts.md) | Ordered row locks prevent overdrafts | Proposed | 2 |
