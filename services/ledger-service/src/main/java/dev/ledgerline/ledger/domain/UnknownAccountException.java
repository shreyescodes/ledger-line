package dev.ledgerline.ledger.domain;

import java.io.Serial;

/** No such account in this tenant's books. Accounts of other tenants are reported the same way. */
public final class UnknownAccountException extends LedgerException {

    @Serial
    private static final long serialVersionUID = 1L;

    public UnknownAccountException(AccountId account) {
        super("no account " + account + " in this tenant's books");
    }
}
