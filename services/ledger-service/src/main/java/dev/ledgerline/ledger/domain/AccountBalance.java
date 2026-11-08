package dev.ledgerline.ledger.domain;

import dev.ledgerline.common.money.CurrencyCode;
import dev.ledgerline.common.money.Money;
import java.util.Objects;

/**
 * The stored balance of an account: the raw (debit-positive) sum of its entries, and a version that
 * goes up by one per entry. Each entry's sequence number on the account is the version it produced.
 */
public record AccountBalance(
        AccountId accountId, TenantId tenantId, AccountType type, CurrencyCode currency, long raw, long version) {

    public AccountBalance {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(currency, "currency");
        if (version < 0) {
            throw new IllegalArgumentException("version can't be negative");
        }
    }

    public static AccountBalance opening(Account account) {
        return new AccountBalance(account.id(), account.tenantId(), account.type(), account.currency(), 0, 0);
    }

    /** The balance in the account's normal direction: what people read (DOMAIN.md §5). */
    public Money ledger() {
        return Money.of(Math.multiplyExact(raw, (long) type.normalSign()), currency);
    }

    /** Applies one entry and returns the new balance; its version is the entry's sequence number. */
    public AccountBalance apply(Side side, long amount) {
        return new AccountBalance(
                accountId, tenantId, type, currency, Math.addExact(raw, side.signed(amount)), Math.addExact(version, 1));
    }
}
