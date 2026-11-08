plugins {
    id("ledgerline.java-library")
}

// Plain Java only: no Spring here (enforced by ArchUnit, see ADR-0008).
dependencies {
    api(libs.uuid.creator)

    testImplementation(project(":libs:test-support"))
}
