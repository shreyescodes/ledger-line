package dev.ledgerline.ledger.application;

import dev.ledgerline.ledger.domain.Account;
import dev.ledgerline.ledger.domain.AccountBalance;
import dev.ledgerline.ledger.domain.AccountId;
import dev.ledgerline.ledger.domain.TenantId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Accounts and their balance projection. Every lookup is scoped to one tenant. */
public interface AccountStore {

    /** Stores a new account together with its zero opening balance. */
    void create(Account account);

    Optional<Account> find(TenantId tenant, AccountId account);

    List<Account> findAll(TenantId tenant, Collection<AccountId> accounts);

    Optional<AccountBalance> balance(TenantId tenant, AccountId account);
}
