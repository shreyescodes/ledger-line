package dev.ledgerline.ledger.domain;

import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * What a posting will write: each entry with its sequence number on its account, and the new
 * balance of every account it touches. Built by {@link PostingRules} from the current balances.
 */
public record PostingPlan(List<PlannedEntry> entries, SortedMap<AccountId, AccountBalance> newBalances) {

    public PostingPlan {
        entries = List.copyOf(entries);
        newBalances = new TreeMap<>(Objects.requireNonNull(newBalances, "newBalances"));
    }

    public record PlannedEntry(EntryDraft draft, long accountSeq) {}
}
