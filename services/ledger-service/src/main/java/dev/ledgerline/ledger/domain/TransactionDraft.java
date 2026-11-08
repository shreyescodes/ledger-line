package dev.ledgerline.ledger.domain;

import dev.ledgerline.common.money.CurrencyCode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * A transaction about to be posted. Construction enforces invariant I2 in code: at least two
 * entries, positive amounts, and debits equal to credits in every currency. (The database checks
 * the same thing again at commit.)
 */
public record TransactionDraft(TenantId tenantId, TransactionType type, String reference, List<EntryDraft> entries) {

    public static final int MAX_ENTRIES = 50;

    public TransactionDraft {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(type, "type");
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
        if (entries.size() < 2) {
            throw new InvalidTransactionException("a transaction needs at least two entries, got " + entries.size());
        }
        if (entries.size() > MAX_ENTRIES) {
            throw new InvalidTransactionException("a transaction can have at most " + MAX_ENTRIES + " entries");
        }
        Map<CurrencyCode, Long> signedTotals = new TreeMap<>((a, b) -> a.code().compareTo(b.code()));
        for (EntryDraft entry : entries) {
            long signed = entry.side().signed(entry.amount().amount());
            signedTotals.merge(entry.amount().currency(), signed, Math::addExact);
        }
        signedTotals.forEach((currency, total) -> {
            if (total != 0) {
                throw new InvalidTransactionException(
                        "debits and credits differ by " + total + " minor units of " + currency);
            }
        });
    }
}
