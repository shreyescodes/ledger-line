package dev.ledgerline.ledger.domain;

import dev.ledgerline.common.money.CurrencyCode;
import java.time.Instant;
import java.util.Objects;

/**
 * One bucket of value in one currency inside one tenant's books (DOMAIN.md §4). {@code holderRef}
 * is an opaque reference to a customer or merchant, never personal data (invariant I11).
 */
public record Account(
        AccountId id, TenantId tenantId, AccountType type, CurrencyCode currency, String holderRef, Instant createdAt) {

    public Account {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
