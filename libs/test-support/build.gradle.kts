plugins {
    id("ledgerline.java-library")
}

// Test helpers shared by all services. Only ever used as a test dependency.
dependencies {
    api("org.springframework.boot:spring-boot-starter-test")
    api("org.springframework.boot:spring-boot-testcontainers")
    api("org.testcontainers:testcontainers-postgresql")
    api(libs.archunit.junit5)
}
