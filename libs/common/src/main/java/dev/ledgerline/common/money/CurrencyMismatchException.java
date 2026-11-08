package dev.ledgerline.common.money;

import java.io.Serial;

/** Thrown when code tries to combine amounts in different currencies. That is always a bug. */
public final class CurrencyMismatchException extends IllegalArgumentException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CurrencyMismatchException(CurrencyCode left, CurrencyCode right) {
        super("can't combine " + left + " with " + right + ": no cross-currency arithmetic");
    }
}
