package dev.ledgerline.ledger.domain;

import dev.ledgerline.common.money.Money;
import java.io.Serial;

/** The posting would take a non-negative account below zero (invariant I5). */
public final class InsufficientFundsException extends LedgerException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InsufficientFundsException(AccountId account, Money resultingBalance) {
        super("account " + account + " can't go below zero; the posting would leave " + resultingBalance);
    }
}
