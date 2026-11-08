package dev.ledgerline.testsupport.property;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A small property-based testing harness: run a property against many generated values, and when
 * one fails, shrink it to a minimal failing value and report the seed that reproduces it.
 *
 * <pre>{@code
 * PropertyCheck.forAll(random -> randomOperations(random))
 *         .tries(200)
 *         .shrinkWith(Shrinkers.list())
 *         .check(operations -> assertThat(replay(operations)).isEqualTo(balances()));
 * }</pre>
 *
 * <p>Each try {@code i} uses seed {@code baseSeed + i}. The base seed is random per run unless
 * {@code -Dproperty.seed=N} is given, so a reported seed replays exactly with
 * {@code -Dproperty.seed=N -Dproperty.tries=1}. {@code -Dproperty.tries} overrides the try count.
 */
public final class PropertyCheck<T> {

    private static final int MAX_SHRINK_STEPS = 1_000;

    private final Function<SplittableRandom, T> generator;
    private int tries = 100;
    private Shrinker<T> shrinker = value -> Stream.empty();

    private PropertyCheck(Function<SplittableRandom, T> generator) {
        this.generator = Objects.requireNonNull(generator, "generator");
    }

    public static <T> PropertyCheck<T> forAll(Function<SplittableRandom, T> generator) {
        return new PropertyCheck<>(generator);
    }

    public PropertyCheck<T> tries(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("tries must be at least 1");
        }
        this.tries = count;
        return this;
    }

    public PropertyCheck<T> shrinkWith(Shrinker<T> valueShrinker) {
        this.shrinker = Objects.requireNonNull(valueShrinker, "shrinker");
        return this;
    }

    /** Runs the property; throws an {@link AssertionError} describing the minimal failure. */
    public void check(Property<T> property) {
        long baseSeed = Long.getLong("property.seed", new SplittableRandom().nextLong());
        int count = Integer.getInteger("property.tries", tries);
        for (int i = 0; i < count; i++) {
            long seed = baseSeed + i;
            T value = generator.apply(new SplittableRandom(seed));
            Optional<Throwable> failure = run(property, value);
            if (failure.isPresent()) {
                throw report(property, value, failure.get(), seed, i + 1, count);
            }
        }
    }

    private AssertionError report(Property<T> property, T original, Throwable firstFailure, long seed, int tryNumber, int count) {
        T smallest = original;
        Throwable smallestFailure = firstFailure;
        int steps = 0;
        boolean shrunk = true;
        while (shrunk && steps < MAX_SHRINK_STEPS) {
            shrunk = false;
            Iterator<T> candidates = shrinker.candidates(smallest).iterator();
            while (candidates.hasNext()) {
                T candidate = candidates.next();
                Optional<Throwable> failure = run(property, candidate);
                if (failure.isPresent()) {
                    smallest = candidate;
                    smallestFailure = failure.get();
                    steps++;
                    shrunk = true;
                    break;
                }
            }
        }
        String nl = System.lineSeparator();
        String message = "Property failed on try " + tryNumber + " of " + count + "." + nl
                + "  seed:   " + seed + "  (replay with -Dproperty.seed=" + seed + " -Dproperty.tries=1)" + nl
                + "  shrunk: " + steps + " step(s)" + nl
                + "  value:  " + smallest + nl
                + "  cause:  " + smallestFailure;
        return new AssertionError(message, smallestFailure);
    }

    private static <T> Optional<Throwable> run(Property<T> property, T value) {
        try {
            property.verify(value);
            return Optional.empty();
        } catch (Exception | AssertionError e) {
            return Optional.of(e);
        }
    }

    /** A property to check. It fails by throwing, usually an AssertionError from AssertJ. */
    @FunctionalInterface
    public interface Property<T> {
        void verify(T value) throws Exception;
    }

    /** Proposes smaller variants of a failing value, most promising first. */
    @FunctionalInterface
    public interface Shrinker<T> {
        Stream<T> candidates(T value);
    }
}
