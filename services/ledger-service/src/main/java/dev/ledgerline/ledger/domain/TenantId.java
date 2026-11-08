package dev.ledgerline.ledger.domain;

import java.util.Objects;
import java.util.UUID;

/** A tenant: the owner of one isolated set of books. */
public record TenantId(UUID value) {

    public TenantId {
        Objects.requireNonNull(value, "value");
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
