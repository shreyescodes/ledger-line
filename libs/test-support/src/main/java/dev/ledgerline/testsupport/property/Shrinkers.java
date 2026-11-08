package dev.ledgerline.testsupport.property;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** Ready-made shrinkers. */
public final class Shrinkers {

    private Shrinkers() {}

    /**
     * Shrinks a list by removing chunks: first halves, then quarters, and so on down to single
     * elements. Good for operation sequences, where the cause is usually a few operations.
     */
    public static <E> PropertyCheck.Shrinker<List<E>> list() {
        return list -> {
            int size = list.size();
            if (size == 0) {
                return Stream.empty();
            }
            Stream<List<E>> result = Stream.empty();
            for (int chunk = Math.max(1, size / 2); chunk >= 1; chunk = chunk == 1 ? 0 : Math.max(1, chunk / 2)) {
                int width = chunk;
                Stream<List<E>> withoutChunks = IntStream.iterate(0, start -> start < size, start -> start + width)
                        .mapToObj(start -> without(list, start, Math.min(size, start + width)));
                result = Stream.concat(result, withoutChunks);
            }
            return result;
        };
    }

    private static <E> List<E> without(List<E> list, int from, int to) {
        List<E> copy = new ArrayList<>(list.subList(0, from));
        copy.addAll(list.subList(to, list.size()));
        return List.copyOf(copy);
    }
}
