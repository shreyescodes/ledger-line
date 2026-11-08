package dev.ledgerline.ledger.adapter.out.persistence;

import dev.ledgerline.ledger.application.PostingContentionException;
import dev.ledgerline.ledger.application.PostingStrategyName;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import org.springframework.dao.ConcurrencyFailureException;

/**
 * Re-runs a whole database transaction when it lost a race: an optimistic version check failed,
 * Postgres reported a serialization failure (40001) or picked it as a deadlock victim (40P01).
 * Waits a little, with jitter, between attempts so the losers don't collide again in lockstep.
 */
final class ConcurrencyRetry {

    private static final Set<String> RETRYABLE_SQL_STATES = Set.of("40001", "40P01");
    private static final long MAX_BACKOFF_MILLIS = 50;

    private final PostingStrategyName strategy;
    private final int maxAttempts;
    private final Counter retries;

    ConcurrencyRetry(PostingStrategyName strategy, int maxAttempts, MeterRegistry meters) {
        this.strategy = strategy;
        this.maxAttempts = maxAttempts;
        this.retries = Counter.builder("ledger.posting.retries")
                .description("Posting attempts that lost a race and were run again")
                .tag("strategy", strategy.name())
                .register(meters);
    }

    <T> T run(Supplier<T> attempt) {
        for (int number = 1; ; number++) {
            try {
                return attempt.get();
            } catch (RuntimeException e) {
                if (!isRetryable(e)) {
                    throw e;
                }
                if (number >= maxAttempts) {
                    throw new PostingContentionException(strategy, number, e);
                }
                retries.increment();
                pause(number);
            }
        }
    }

    static boolean isRetryable(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConcurrencyFailureException) {
                return true;
            }
            if (cause instanceof SQLException sql && RETRYABLE_SQL_STATES.contains(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    private static void pause(int attempt) {
        long ceiling = Math.min(MAX_BACKOFF_MILLIS, 1L << Math.min(attempt, 6));
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(ceiling + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting to retry a posting", e);
        }
    }
}
