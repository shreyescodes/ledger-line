package dev.ledgerline.ledger.application;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

/** The verifier's findings: for each invariant check, how many violations it found (0 is good). */
public record InvariantReport(Instant checkedAt, Map<String, Long> violations) {

    public static final String BALANCES_MATCH_ENTRIES = "balances_match_entries";
    public static final String BOOKS_SUM_TO_ZERO = "books_sum_to_zero";
    public static final String NO_OVERDRAWN_ACCOUNTS = "no_overdrawn_accounts";
    public static final String TRANSACTIONS_BALANCE = "transactions_balance";
    public static final String SEQUENCES_MATCH_VERSIONS = "sequences_match_versions";

    public InvariantReport {
        violations = new TreeMap<>(violations);
    }

    public boolean ok() {
        return violations.values().stream().allMatch(count -> count == 0);
    }
}
