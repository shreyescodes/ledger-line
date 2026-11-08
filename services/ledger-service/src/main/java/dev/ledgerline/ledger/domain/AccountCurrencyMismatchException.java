package dev.ledgerline.ledger.domain;

import dev.ledgerline.common.money.CurrencyCode;
import java.io.Serial;

/** An entry's currency differs from its account's currency. */
public final class AccountCurrencyMismatchException extends LedgerException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AccountCurrencyMismatchException(AccountId account, CurrencyCode accountCurrency, CurrencyCode entryCurrency) {
        super("account " + account + " holds " + accountCurrency + " but the entry is in " + entryCurrency);
    }
}
