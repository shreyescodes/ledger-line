# ADR-0007: Gradle multi-module build with convention plugins and locked dependencies

- **Status:** Proposed
- **Date:** 2026-11-06
- **Stage:** 1

## Context

One repository holds five services and two shared libraries. They must build
the same way, share one set of library versions, and pass the same quality
gates: formatter, static analysis and tests. The brief asks for Gradle (Kotlin
DSL), the current Java LTS, and versions pinned from official sources.

## Decision

1. **One Gradle build, one subproject per module** (`libs/*`, `services/*`),
   declared in `settings.gradle.kts`.
2. **Shared setup lives in convention plugins** in the `build-logic` included
   build:
   - `ledgerline.java-conventions` covers the toolchain, versions, formatting,
     static analysis, tests and locking.
   - `ledgerline.java-library` adds the library setup.
   - `ledgerline.spring-boot-service` adds web, Actuator and Prometheus.

   A module's own build file only lists what is specific to it.
3. **Versions.** Spring Boot's BOM (`spring-boot-dependencies`, imported as a
   Gradle `platform`) decides the version of everything it manages, so those
   libraries are tested together. Everything else is pinned once in
   `gradle/libs.versions.toml`. We use Gradle's native platform support
   instead of the `io.spring.dependency-management` plugin.
4. **Java 25 (current LTS) through a Gradle toolchain.** Gradle can run on any
   JDK 17+, and the foojay resolver downloads JDK 25 when it isn't installed.
5. **Dependency locking** for every configuration. `gradle.lockfile` pins the
   full transitive graph, so builds are reproducible and Trivy can scan the
   exact versions. Refresh with `make lock`.
6. **Quality gates run in every build:**
   - Spotless with palantir-java-format (formatting);
   - Error Prone (bug patterns at compile time);
   - `javac -Xlint:all -Werror` (warnings are errors).
7. **The Gradle wrapper pins its distribution** with `distributionSha256Sum`.

Versions on 2026-11-06, taken from the Gradle version service, Maven Central,
the Gradle plugin portal and the Adoptium API: Gradle 9.8.2, Spring Boot
4.1.2 (4.2.0 was still at RC2), Java 25.0.5, ArchUnit 1.5.1, Error Prone
2.52.0, palantir-java-format 2.103.0.

## Alternatives considered

- **Maven**: the brief asks for Gradle. Gradle's toolchains, convention plugins
  and configuration cache also suit a multi-module repo well.
- **`buildSrc`** instead of an included build: any change to it invalidates the
  whole build, and it can't be reused as cleanly.
- **`io.spring.dependency-management` plugin**: works, but duplicates what
  Gradle platforms now do natively.
- **Gradle dependency verification** (checksums for every artifact): stronger
  supply-chain protection, but high upkeep on every version bump. We may
  revisit it after Stage 11.
- **google-java-format**: also fine. Palantir's 120-column style suits Spring's
  long type names better.

## Consequences

- Adding a dependency means refreshing the lockfiles (`make lock`), or the
  build fails. That's deliberate: every version change shows up in review.
- `-Werror` plus Error Prone make the first build of new code stricter than
  usual. Fixing warnings early is cheaper than living with them.
- On machines where IPv6 to GitHub's CDN is broken, the user-level
  `~/.gradle/gradle.properties` needs `-Djava.net.preferIPv4Stack=true` (see the
  README). Nothing in the repo depends on it.
