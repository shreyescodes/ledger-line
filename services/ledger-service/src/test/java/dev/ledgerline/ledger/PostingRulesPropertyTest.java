package dev.ledgerline.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ledgerline.common.money.CurrencyCode;
import dev.ledgerline.common.money.Money;
import dev.ledgerline.ledger.PostingScenarios.AccountSpec;
import dev.ledgerline.ledger.PostingScenarios.Model;
import dev.ledgerline.ledger.PostingScenarios.Op;
import dev.ledgerline.ledger.PostingScenarios.Outcome;
import dev.ledgerline.ledger.domain.AccountBalance;
import dev.ledgerline.ledger.domain.AccountCurrencyMismatchException;
import dev.ledgerline.ledger.domain.AccountId;
import dev.ledgerline.ledger.domain.EntryDraft;
import dev.ledgerline.ledger.domain.InsufficientFundsException;
import dev.ledgerline.ledger.domain.PostingPlan;
import dev.ledgerline.ledger.domain.PostingRules;
import dev.ledgerline.ledger.domain.TenantId;
import dev.ledgerline.ledger.domain.TransactionDraft;
import dev.ledgerline.ledger.domain.TransactionType;
import dev.ledgerline.testsupport.property.PropertyCheck;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * The three ledger properties from the brief, checked on the pure posting rules with hundreds of
 * random scenarios. No database, so it runs anywhere in milliseconds:
 *
 * <ol>
 *   <li>the books sum to zero in every currency;
 *   <li>no non-negative account ever goes below zero;
 *   <li>replaying the accepted entries reproduces every balance.
 * </ol>
 */
class PostingRulesPropertyTest {

    @Test
    void anySequenceOfPostingsKeepsTheBooksBalanced() {
        PropertyCheck.forAll(PostingScenarios::generate)
                .tries(500)
                .shrinkWith(PostingScenarios.shrinker())
                .check(scenario -> {
                    TenantId tenant = new TenantId(UUID.randomUUID());
                    List<AccountId> ids = new ArrayList<>();
                    Map<AccountId, AccountBalance> balances = new HashMap<>();
                    for (AccountSpec spec : scenario.accounts()) {
                        AccountId id = new AccountId(UUID.randomUUID());
                        ids.add(id);
                        balances.put(
                                id,
                                new AccountBalance(id, tenant, spec.type(), CurrencyCode.of(spec.currency()), 0, 0));
                    }
                    Model model = new Model(scenario.accounts());
                    List<EntryDraft> accepted = new ArrayList<>();

                    for (Op op : scenario.ops()) {
                        List<EntryDraft> entries = op.legs().stream()
                                .map(leg -> new EntryDraft(
                                        ids.get(leg.account()), leg.side(), Money.of(leg.amount(), leg.currency())))
                                .toList();
                        Outcome expected = model.apply(op);
                        Outcome actual;
                        try {
                            PostingPlan plan = PostingRules.plan(
                                    new TransactionDraft(tenant, TransactionType.ADJUSTMENT, null, entries), balances);
                            balances.putAll(plan.newBalances());
                            accepted.addAll(entries);
                            actual = Outcome.POSTED;
                        } catch (InsufficientFundsException e) {
                            actual = Outcome.INSUFFICIENT_FUNDS;
                        } catch (AccountCurrencyMismatchException e) {
                            actual = Outcome.CURRENCY_MISMATCH;
                        }
                        assertThat(actual).as("outcome of %s", op).isEqualTo(expected);
                    }

                    // 1. The books sum to zero in every currency.
                    Map<CurrencyCode, Long> sums = new HashMap<>();
                    balances.values().forEach(b -> sums.merge(b.currency(), b.raw(), Math::addExact));
                    assertThat(sums.values()).allMatch(sum -> sum == 0);
                    // 2. No non-negative account below zero.
                    assertThat(balances.values())
                            .filteredOn(b -> b.type().nonNegative())
                            .allSatisfy(b -> assertThat(b.ledger().amount()).isNotNegative());
                    // 3. Replaying the accepted entries reproduces every balance, and the oracle agrees.
                    Map<AccountId, Long> replayed = new HashMap<>();
                    accepted.forEach(e -> replayed.merge(
                            e.account(), e.side().signed(e.amount().amount()), Math::addExact));
                    for (int i = 0; i < ids.size(); i++) {
                        AccountId id = ids.get(i);
                        AccountBalance balance = balances.get(id);
                        assertThat(balance.raw()).isEqualTo(replayed.getOrDefault(id, 0L));
                        assertThat(balance.raw()).isEqualTo(model.raw[i]);
                        long entriesOnAccount = accepted.stream()
                                .filter(e -> e.account().equals(id))
                                .count();
                        assertThat(balance.version())
                                .as("one version step per entry")
                                .isEqualTo(entriesOnAccount);
                    }
                });
    }
}
