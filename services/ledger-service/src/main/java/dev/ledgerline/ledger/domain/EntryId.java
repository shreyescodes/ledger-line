package dev.ledgerline.ledger.domain;

import java.util.Objects;
import java.util.UUID;

public record EntryId(UUID value) {

    public EntryId {
        Objects.requireNonNull(value, "value");
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
