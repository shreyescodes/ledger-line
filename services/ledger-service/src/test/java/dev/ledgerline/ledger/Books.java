package dev.ledgerline.ledger;

import dev.ledgerline.common.money.CurrencyCode;
import dev.ledgerline.common.money.Money;
import dev.ledgerline.ledger.application.LedgerService;
import dev.ledgerline.ledger.domain.Account;
import dev.ledgerline.ledger.domain.AccountType;
import dev.ledgerline.ledger.domain.EntryDraft;
import dev.ledgerline.ledger.domain.TenantId;
import dev.ledgerline.ledger.domain.TransactionDraft;
import dev.ledgerline.ledger.domain.TransactionType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A fresh tenant's books for one test, so tests never share accounts or need cleanup. */
final class Books {

    final TenantId tenant = new TenantId(UUID.randomUUID());
    private final LedgerService ledger;
    private final Map<CurrencyCode, Account> clearing = new HashMap<>();

    Books(LedgerService ledger) {
        this.ledger = ledger;
    }

    Account open(AccountType type, String currency) {
        return ledger.openAccount(tenant, type, CurrencyCode.of(currency), null);
    }

    /** Money arriving from the PSP, posted with the configured strategy. */
    void fund(Account account, long amount) {
        fund(account, amount, ledger::post);
    }

    /** Money arriving from the PSP: debit PSP clearing, credit the (liability) account. */
    void fund(Account account, long amount, Poster strategy) {
        Account psp = clearing.computeIfAbsent(account.currency(), c -> open(AccountType.PSP_CLEARING, c.code()));
        Money money = Money.of(amount, account.currency());
        strategy.post(new TransactionDraft(
                tenant,
                TransactionType.TOP_UP,
                "fund",
                List.of(EntryDraft.debit(psp.id(), money), EntryDraft.credit(account.id(), money))));
    }

    /** Anything that posts a draft: a specific strategy, or the service's configured one. */
    @FunctionalInterface
    interface Poster {
        void post(TransactionDraft draft);
    }

    /** Wallet to wallet: debit the payer, credit the payee. */
    TransactionDraft transfer(Account from, Account to, long amount) {
        Money money = Money.of(amount, from.currency());
        return new TransactionDraft(
                tenant,
                TransactionType.TRANSFER,
                null,
                List.of(EntryDraft.debit(from.id(), money), EntryDraft.credit(to.id(), money)));
    }
}
