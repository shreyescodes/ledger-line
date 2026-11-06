// A deployable Spring Boot service with health, readiness and Prometheus metrics.

plugins {
    id("ledgerline.java-conventions")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":libs:common"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

    testImplementation(project(":libs:test-support"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

// Only the executable jar is needed, with a stable name for the Dockerfile.
tasks.named<Jar>("jar") {
    enabled = false
}
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName = "${project.name}.jar"
}
