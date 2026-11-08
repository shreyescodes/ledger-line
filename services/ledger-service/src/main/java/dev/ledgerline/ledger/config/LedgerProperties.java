package dev.ledgerline.ledger.config;

import dev.ledgerline.ledger.application.PostingStrategyName;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** {@code ledger.*} settings. */
@ConfigurationProperties("ledger")
public record LedgerProperties(
        @DefaultValue Posting posting, @DefaultValue Verifier verifier) {

    /**
     * How postings are made.
     *
     * @param strategy which overdraft-prevention strategy postings use (ADR-0014)
     * @param maxAttempts how often a posting that lost a race is tried before giving up
     */
    public record Posting(
            @DefaultValue("ORDERED_LOCKS") PostingStrategyName strategy,
            @DefaultValue("10") int maxAttempts) {}

    /**
     * The scheduled invariant verifier.
     *
     * @param enabled whether the scheduled invariant verifier runs
     * @param interval pause between verifier runs
     */
    public record Verifier(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("PT1M") Duration interval) {}
}
