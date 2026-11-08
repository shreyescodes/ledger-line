package dev.ledgerline.ledger.domain;

import java.time.Instant;
import java.util.List;

public record PostedTransaction(
        TransactionId id,
        TenantId tenantId,
        TransactionType type,
        String reference,
        Instant recordedAt,
        List<PostedEntry> entries) {

    public PostedTransaction {
        entries = List.copyOf(entries);
    }
}
