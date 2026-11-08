package dev.ledgerline.ledger.application;

import dev.ledgerline.ledger.domain.TenantId;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Proves the invariants hold over the stored data (I2, I4, I5, I12): the balance projection equals
 * the sum of the entries, every tenant's books sum to zero per currency, no non-negative account
 * is overdrawn, and every transaction balances.
 */
public final class InvariantVerifier {

    private final InvariantQueries queries;
    private final Clock clock;

    public InvariantVerifier(InvariantQueries queries, Clock clock) {
        this.queries = queries;
        this.clock = clock;
    }

    public InvariantReport verifyAll() {
        return new InvariantReport(Instant.now(clock), queries.countViolations(Optional.empty()));
    }

    public InvariantReport verify(TenantId tenant) {
        return new InvariantReport(Instant.now(clock), queries.countViolations(Optional.of(tenant)));
    }
}
