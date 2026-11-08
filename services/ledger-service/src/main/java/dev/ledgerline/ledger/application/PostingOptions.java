package dev.ledgerline.ledger.application;

/** Tuning shared by the posting strategies. */
public record PostingOptions(int maxAttempts) {

    public PostingOptions {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
    }
}
