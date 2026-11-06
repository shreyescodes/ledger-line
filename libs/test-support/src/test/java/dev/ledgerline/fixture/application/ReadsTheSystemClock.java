package dev.ledgerline.fixture.application;

import java.time.Instant;

/** Breaks I10: reads the system clock instead of using an injected Clock. */
public final class ReadsTheSystemClock {

    public Instant stamp() {
        return Instant.now();
    }
}
