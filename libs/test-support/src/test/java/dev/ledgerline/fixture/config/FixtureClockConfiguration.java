package dev.ledgerline.fixture.config;

import java.time.Clock;

/** Allowed: configuration is the one place that reads the system clock. */
public final class FixtureClockConfiguration {

    public Clock clock() {
        return Clock.systemUTC();
    }
}
