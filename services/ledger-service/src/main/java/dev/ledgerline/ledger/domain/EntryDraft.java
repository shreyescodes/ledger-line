package dev.ledgerline.ledger.domain;

import dev.ledgerline.common.money.Money;
import java.util.Objects;

/** One line of a transaction that hasn't been posted yet. */
public record EntryDraft(AccountId account, Side side, Money amount) {

    public EntryDraft {
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(amount, "amount");
        if (!amount.isPositive()) {
            throw new InvalidTransactionException("entry amounts must be positive, got " + amount);
        }
    }

    public static EntryDraft debit(AccountId account, Money amount) {
        return new EntryDraft(account, Side.DEBIT, amount);
    }

    public static EntryDraft credit(AccountId account, Money amount) {
        return new EntryDraft(account, Side.CREDIT, amount);
    }
}
