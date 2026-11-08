package dev.ledgerline.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ledgerline.common.money.Money;
import dev.ledgerline.ledger.PostingScenarios.AccountSpec;
import dev.ledgerline.ledger.PostingScenarios.Model;
import dev.ledgerline.ledger.PostingScenarios.Op;
import dev.ledgerline.ledger.PostingScenarios.Outcome;
import dev.ledgerline.ledger.application.InvariantReport;
import dev.ledgerline.ledger.application.InvariantVerifier;
import dev.ledgerline.ledger.application.LedgerService;
import dev.ledgerline.ledger.application.PostingStrategy;
import dev.ledgerline.ledger.domain.Account;
import dev.ledgerline.ledger.domain.AccountCurrencyMismatchException;
import dev.ledgerline.ledger.domain.EntryDraft;
import dev.ledgerline.ledger.domain.InsufficientFundsException;
import dev.ledgerline.ledger.domain.TransactionDraft;
import dev.ledgerline.ledger.domain.TransactionType;
import dev.ledgerline.testsupport.property.PropertyCheck;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * The same three properties as {@link PostingRulesPropertyTest}, but end to end: every random
 * scenario is posted to real Postgres through a randomly chosen strategy, then the stored data is
 * checked. Each scenario gets a fresh tenant, so shrinking can re-run it freely.
 */
@LedgerIntegrationTest
class LedgerPropertiesIntegrationTest {

    @Autowired
    private LedgerService ledger;

    @Autowired
    private List<PostingStrategy> strategies;

    @Autowired
    private InvariantVerifier verifier;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void anySequenceOfPostingsKeepsTheStoredBooksBalanced() {
        PropertyCheck.forAll(PostingScenarios::generate)
                .tries(60)
                .shrinkWith(PostingScenarios.shrinker())
                .check(scenario -> {
                    PostingStrategy strategy = strategies.stream()
                            .filter(candidate -> candidate.name() == scenario.strategy())
                            .findFirst()
                            .orElseThrow();
                    Books books = new Books(ledger);
                    List<Account> accounts = new ArrayList<>();
                    for (AccountSpec spec : scenario.accounts()) {
                        accounts.add(books.open(spec.type(), spec.currency()));
                    }
                    Model model = new Model(scenario.accounts());

                    for (Op op : scenario.ops()) {
                        TransactionDraft draft = new TransactionDraft(
                                books.tenant,
                                TransactionType.ADJUSTMENT,
                                null,
                                op.legs().stream()
                                        .map(leg -> new EntryDraft(
                                                accounts.get(leg.account()).id(),
                                                leg.side(),
                                                Money.of(leg.amount(), leg.currency())))
                                        .toList());
                        Outcome expected = model.apply(op);
                        Outcome actual;
                        try {
                            strategy.post(draft);
                            actual = Outcome.POSTED;
                        } catch (InsufficientFundsException e) {
                            actual = Outcome.INSUFFICIENT_FUNDS;
                        } catch (AccountCurrencyMismatchException e) {
                            actual = Outcome.CURRENCY_MISMATCH;
                        }
                        assertThat(actual).as("outcome of %s", op).isEqualTo(expected);
                    }

                    // Stored balances equal the oracle's, and no non-negative account is below zero.
                    for (int i = 0; i < accounts.size(); i++) {
                        var balance = ledger.account(books.tenant, accounts.get(i).id()).balance();
                        assertThat(balance.raw()).as("raw balance of account %d", i).isEqualTo(model.raw[i]);
                        if (balance.type().nonNegative()) {
                            assertThat(balance.ledger().amount()).isNotNegative();
                        }
                    }
                    // Replaying the stored entries reproduces the stored balances.
                    Map<UUID, Long> replayed = jdbc.sql("""
                                    SELECT account_id, sum(CASE side WHEN 'DEBIT' THEN amount ELSE -amount END) AS raw
                                    FROM entry WHERE tenant_id = :tenant GROUP BY account_id
                                    """)
                            .param("tenant", books.tenant.value())
                            .query((rs, n) -> Map.entry(rs.getObject("account_id", UUID.class), rs.getLong("raw")))
                            .list()
                            .stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    for (int i = 0; i < accounts.size(); i++) {
                        assertThat(replayed.getOrDefault(accounts.get(i).id().value(), 0L))
                                .as("replayed entries of account %d", i)
                                .isEqualTo(model.raw[i]);
                    }
                    Long entries = jdbc.sql("SELECT count(*) FROM entry WHERE tenant_id = :tenant")
                            .param("tenant", books.tenant.value())
                            .query(Long.class)
                            .single();
                    assertThat(entries).as("refused postings leave no trace").isEqualTo((long) model.postedLegs);
                    // The verifier's checks, including "the books sum to zero per currency".
                    InvariantReport report = verifier.verify(books.tenant);
                    assertThat(report.ok()).as("invariants: %s", report.violations()).isTrue();
                });
    }
}
