pluginManagement {
    includeBuild("build-logic")
}

plugins {
    // Downloads the Java toolchain (JDK 25) if it isn't installed locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "ledgerline"

include(
    "libs:common",
    "libs:test-support",
    "services:ledger-service",
    "services:payment-service",
    "services:mock-psp",
    "services:reconciliation-service",
    "services:notification-service",
)
