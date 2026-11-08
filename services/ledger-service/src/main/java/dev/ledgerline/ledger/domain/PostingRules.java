package dev.ledgerline.ledger.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The rules every posting strategy shares, as a pure function: given the current balances of the
 * accounts a transaction touches, work out the new balances or refuse the posting.
 *
 * <p>The strategies differ only in how they read and write those balances safely under
 * concurrency (row locks, optimistic versions or serializable isolation).
 */
public final class PostingRules {

    private PostingRules() {}

    /**
     * Plans a posting.
     *
     * @param current the current balances of (at least) every account in the draft
     * @throws UnknownAccountException if an account is missing or belongs to another tenant
     * @throws AccountCurrencyMismatchException if an entry's currency differs from its account's
     * @throws InsufficientFundsException if a non-negative account would go below zero
     */
    public static PostingPlan plan(TransactionDraft draft, Map<AccountId, AccountBalance> current) {
        SortedMap<AccountId, AccountBalance> balances = new TreeMap<>();
        List<PostingPlan.PlannedEntry> planned = new ArrayList<>();
        for (EntryDraft entry : draft.entries()) {
            AccountBalance balance = balances.containsKey(entry.account())
                    ? balances.get(entry.account())
                    : current.get(entry.account());
            if (balance == null || !balance.tenantId().equals(draft.tenantId())) {
                throw new UnknownAccountException(entry.account());
            }
            if (!balance.currency().equals(entry.amount().currency())) {
                throw new AccountCurrencyMismatchException(
                        entry.account(), balance.currency(), entry.amount().currency());
            }
            AccountBalance updated = balance.apply(entry.side(), entry.amount().amount());
            balances.put(entry.account(), updated);
            planned.add(new PostingPlan.PlannedEntry(entry, updated.version()));
        }
        // Checked after all entries: a transaction may debit and credit the same account.
        for (AccountBalance balance : balances.values()) {
            if (balance.type().nonNegative() && balance.ledger().isNegative()) {
                throw new InsufficientFundsException(balance.accountId(), balance.ledger());
            }
        }
        return new PostingPlan(planned, balances);
    }
}
