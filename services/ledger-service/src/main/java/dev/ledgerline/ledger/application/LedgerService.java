package dev.ledgerline.ledger.application;

import dev.ledgerline.common.id.IdGenerator;
import dev.ledgerline.common.money.CurrencyCode;
import dev.ledgerline.common.money.Money;
import dev.ledgerline.ledger.domain.Account;
import dev.ledgerline.ledger.domain.AccountBalance;
import dev.ledgerline.ledger.domain.AccountId;
import dev.ledgerline.ledger.domain.AccountType;
import dev.ledgerline.ledger.domain.EntryDraft;
import dev.ledgerline.ledger.domain.InvalidTransactionException;
import dev.ledgerline.ledger.domain.PostedTransaction;
import dev.ledgerline.ledger.domain.Side;
import dev.ledgerline.ledger.domain.TenantId;
import dev.ledgerline.ledger.domain.TransactionDraft;
import dev.ledgerline.ledger.domain.TransactionType;
import dev.ledgerline.ledger.domain.UnknownAccountException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The ledger's use cases. Postings go through whichever {@link PostingStrategy} is configured. */
public final class LedgerService {

    public static final int MAX_STATEMENT_PAGE = 200;

    private final AccountStore accounts;
    private final PostingStrategy posting;
    private final StatementQuery statements;
    private final IdGenerator ids;
    private final Clock clock;

    public LedgerService(
            AccountStore accounts, PostingStrategy posting, StatementQuery statements, IdGenerator ids, Clock clock) {
        this.accounts = accounts;
        this.posting = posting;
        this.statements = statements;
        this.ids = ids;
        this.clock = clock;
    }

    public PostingStrategyName postingStrategy() {
        return posting.name();
    }

    public Account openAccount(TenantId tenant, AccountType type, CurrencyCode currency, String holderRef) {
        Account account = new Account(new AccountId(ids.next()), tenant, type, currency, holderRef, Instant.now(clock));
        accounts.create(account);
        return account;
    }

    public PostedTransaction post(TransactionDraft draft) {
        return posting.post(draft);
    }

    /**
     * Moves value from one account to another of the same category, e.g. wallet to wallet. For
     * credit-normal accounts that is a debit of {@code from}; for debit-normal ones, a credit.
     */
    public PostedTransaction transfer(TenantId tenant, AccountId from, AccountId to, Money amount, String reference) {
        if (from.equals(to)) {
            throw new InvalidTransactionException("can't transfer from an account to itself");
        }
        Map<AccountId, Account> found = accounts.findAll(tenant, List.of(from, to)).stream()
                .collect(Collectors.toMap(Account::id, Function.identity()));
        Account source = found.get(from);
        Account target = found.get(to);
        if (source == null) {
            throw new UnknownAccountException(from);
        }
        if (target == null) {
            throw new UnknownAccountException(to);
        }
        if (source.type().category() != target.type().category()) {
            throw new InvalidTransactionException("a transfer moves value between accounts of the same category; "
                    + source.type() + " and " + target.type() + " differ, so post a transaction instead");
        }
        Side decrease = source.type().category().normalSide().opposite();
        TransactionDraft draft = new TransactionDraft(
                tenant,
                TransactionType.TRANSFER,
                reference,
                List.of(new EntryDraft(from, decrease, amount), new EntryDraft(to, decrease.opposite(), amount)));
        return posting.post(draft);
    }

    public AccountView account(TenantId tenant, AccountId id) {
        Account account = accounts.find(tenant, id).orElseThrow(() -> new UnknownAccountException(id));
        AccountBalance balance = accounts.balance(tenant, id).orElseThrow(() -> new UnknownAccountException(id));
        return new AccountView(account, balance);
    }

    public StatementPage statement(TenantId tenant, AccountId id, long afterSeq, int limit) {
        if (accounts.find(tenant, id).isEmpty()) {
            throw new UnknownAccountException(id);
        }
        int pageSize = Math.clamp(limit, 1, MAX_STATEMENT_PAGE);
        return statements.page(tenant, id, Math.max(0, afterSeq), pageSize);
    }
}
