package dev.ledgerline.common.id;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TimeOrderedIdGeneratorTest {

    private static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");

    @Test
    void makesVersion7IdsStampedWithTheInjectedClock() {
        UUID id = new TimeOrderedIdGenerator(Clock.fixed(NOW, ZoneOffset.UTC)).next();

        assertThat(id.version()).isEqualTo(7);
        assertThat(id.getMostSignificantBits() >>> 16).as("48-bit millisecond timestamp").isEqualTo(NOW.toEpochMilli());
    }

    @Test
    void idsFromTheSameMillisecondStillSortInCreationOrder() {
        IdGenerator ids = new TimeOrderedIdGenerator(Clock.fixed(NOW, ZoneOffset.UTC));
        List<UUID> created = new ArrayList<>();
        for (int i = 0; i < 1_000; i++) {
            created.add(ids.next());
        }

        // Postgres compares uuid values as unsigned bytes, which is what this comparator does too.
        List<UUID> sorted = created.stream()
                .sorted((a, b) -> {
                    int high = Long.compareUnsigned(a.getMostSignificantBits(), b.getMostSignificantBits());
                    return high != 0 ? high : Long.compareUnsigned(a.getLeastSignificantBits(), b.getLeastSignificantBits());
                })
                .toList();
        assertThat(sorted).containsExactlyElementsOf(created);
        assertThat(created).doesNotHaveDuplicates();
    }
}
