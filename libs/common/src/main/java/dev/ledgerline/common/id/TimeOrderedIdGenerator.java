package dev.ledgerline.common.id;

import com.github.f4b6a3.uuid.factory.standard.TimeOrderedEpochFactory;
import java.time.Clock;
import java.util.UUID;

/**
 * UUIDv7 ids (RFC 9562): a millisecond timestamp followed by random bits, so ids sort by creation
 * time and index well. Ids made in the same millisecond stay ordered too ("plus 1" increment).
 *
 * <p>The timestamp comes from the injected {@link Clock} (invariant I10).
 */
public final class TimeOrderedIdGenerator implements IdGenerator {

    private final TimeOrderedEpochFactory factory;

    public TimeOrderedIdGenerator(Clock clock) {
        this.factory = TimeOrderedEpochFactory.builder()
                .withClock(clock)
                .withIncrementPlus1()
                .build();
    }

    @Override
    public UUID next() {
        return factory.create();
    }
}
