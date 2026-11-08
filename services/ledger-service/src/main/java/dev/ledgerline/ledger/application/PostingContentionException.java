package dev.ledgerline.ledger.application;

import java.io.Serial;

/** Concurrent postings kept conflicting and the retry budget ran out. Nothing was written. */
public final class PostingContentionException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public PostingContentionException(PostingStrategyName strategy, int attempts, Throwable lastConflict) {
        super(strategy + " posting still conflicted after " + attempts + " attempts", lastConflict);
    }
}
