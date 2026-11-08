package dev.ledgerline.ledger.domain;

import java.io.Serial;

/** A posting the ledger refuses. Each subtype maps to one problem type in the API. */
public abstract class LedgerException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    protected LedgerException(String message) {
        super(message);
    }
}
