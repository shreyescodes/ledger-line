package dev.ledgerline.testsupport.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** The harness itself must find failures, shrink them, and report a seed that replays them. */
class PropertyCheckTest {

    private static List<Integer> randomList(SplittableRandom random) {
        int size = random.nextInt(0, 40);
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            values.add(random.nextInt(0, 10));
        }
        return values;
    }

    @Test
    void runsEveryTryWhenThePropertyHolds() {
        AtomicInteger runs = new AtomicInteger();

        PropertyCheck.forAll(PropertyCheckTest::randomList).tries(50).check(values -> runs.incrementAndGet());

        assertThat(runs).hasValue(50);
    }

    @Test
    void shrinksAFailureToTheSmallestCounterexample() {
        assertThatThrownBy(() -> PropertyCheck.forAll(PropertyCheckTest::randomList)
                        .tries(500)
                        .shrinkWith(Shrinkers.list())
                        .check(values -> assertThat(values).doesNotContain(7)))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("value:  [7]")
                .hasMessageContaining("-Dproperty.seed=");
    }

    @Test
    void theReportedSeedReproducesTheFailingValue() {
        List<List<Integer>> failing = new ArrayList<>();
        long seed = 0;
        try {
            PropertyCheck.forAll(PropertyCheckTest::randomList).tries(500).check(values -> {
                if (values.contains(7)) {
                    failing.add(values);
                    throw new AssertionError("contains 7");
                }
            });
        } catch (AssertionError e) {
            String message = e.getMessage();
            int start = message.indexOf("seed:   ") + "seed:   ".length();
            seed = Long.parseLong(message.substring(start, message.indexOf(' ', start)));
        }

        assertThat(failing).isNotEmpty();
        assertThat(randomList(new SplittableRandom(seed))).isEqualTo(failing.get(0));
    }
}
