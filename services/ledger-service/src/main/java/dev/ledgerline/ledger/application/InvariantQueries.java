package dev.ledgerline.ledger.application;

import dev.ledgerline.ledger.domain.TenantId;
import java.util.Map;
import java.util.Optional;

/** Counts invariant violations straight from the stored data, in one consistent snapshot. */
public interface InvariantQueries {

    /** Violation counts keyed by the check names in {@link InvariantReport}. */
    Map<String, Long> countViolations(Optional<TenantId> tenant);
}
