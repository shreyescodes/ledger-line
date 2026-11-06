package dev.ledgerline.fixture.application;

import java.time.Clock;
import java.time.Instant;

/** Follows I10: time comes from the Clock it is given. */
public final class UsesAnInjectedClock {

    private final Clock clock;

    public UsesAnInjectedClock(Clock clock) {
        this.clock = clock;
    }

    public Instant stamp() {
        return Instant.now(clock);
    }
}
