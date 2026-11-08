package dev.ledgerline.ledger.domain;

import java.io.Serial;

/** The transaction itself is malformed: too few entries, unbalanced, non-positive amounts. */
public final class InvalidTransactionException extends LedgerException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidTransactionException(String message) {
        super(message);
    }
}
