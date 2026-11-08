plugins {
    id("ledgerline.spring-boot-service")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
}

// Benchmarks are tests tagged "benchmark". They never run with `test`; run them on purpose with
// ./gradlew :services:ledger-service:benchmark (needs Docker). Results land in build/benchmark.
tasks.named<Test>("test") {
    useJUnitPlatform { excludeTags("benchmark") }
}

val benchmark by tasks.registering(Test::class) {
    description = "Benchmarks the three overdraft-prevention strategies against Postgres (needs Docker)."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform { includeTags("benchmark") }
    systemProperty("benchmark.output", layout.buildDirectory.dir("benchmark").get().asFile.absolutePath)
    maxHeapSize = "1g"
    outputs.upToDateWhen { false }
}
