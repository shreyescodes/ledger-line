package dev.ledgerline.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ledgerline.ledger.application.InvariantReport;
import dev.ledgerline.ledger.application.InvariantVerifier;
import dev.ledgerline.ledger.application.LedgerService;
import dev.ledgerline.ledger.application.PostingContentionException;
import dev.ledgerline.ledger.application.PostingStrategy;
import dev.ledgerline.ledger.application.PostingStrategyName;
import dev.ledgerline.ledger.domain.Account;
import dev.ledgerline.ledger.domain.AccountType;
import dev.ledgerline.ledger.domain.InsufficientFundsException;
import dev.ledgerline.ledger.domain.TransactionDraft;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Stage 2 acceptance: 1,000 transfers fired in parallel across 10 accounts, with every strategy,
 * must leave every invariant intact. Amounts are large enough that many transfers hit the
 * overdraft rule, so the race between "check the balance" and "debit it" is really exercised.
 */
@LedgerIntegrationTest
class ConcurrentTransfersIntegrationTest {

    private static final int ACCOUNTS = 10;
    private static final int TRANSFERS = 1_000;
    private static final int THREADS = 32;
    private static final long OPENING_BALANCE = 10_000;

    @Autowired
    private LedgerService ledger;

    @Autowired
    private List<PostingStrategy> strategies;

    @Autowired
    private InvariantVerifier verifier;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private MeterRegistry meters;

    @ParameterizedTest(name = "{0}")
    @EnumSource(PostingStrategyName.class)
    void thousandParallelTransfersAcrossTenAccountsKeepEveryInvariant(PostingStrategyName name) throws Exception {
        PostingStrategy strategy = strategies.stream()
                .filter(candidate -> candidate.name() == name)
                .findFirst()
                .orElseThrow();
        Books books = new Books(ledger);
        List<Account> wallets = new ArrayList<>();
        for (int i = 0; i < ACCOUNTS; i++) {
            Account wallet = books.open(AccountType.CUSTOMER_WALLET, "INR");
            books.fund(wallet, OPENING_BALANCE, strategy::post);
            wallets.add(wallet);
        }
        Random random = new Random(42);
        List<TransactionDraft> transfers = new ArrayList<>();
        for (int i = 0; i < TRANSFERS; i++) {
            int from = random.nextInt(ACCOUNTS);
            int to = (from + 1 + random.nextInt(ACCOUNTS - 1)) % ACCOUNTS;
            transfers.add(books.transfer(wallets.get(from), wallets.get(to), 1 + random.nextInt(3_000)));
        }
        double retriesBefore = retries(name);

        AtomicInteger posted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        AtomicInteger gaveUp = new AtomicInteger();
        Queue<Throwable> unexpected = new ConcurrentLinkedQueue<>();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        for (TransactionDraft transfer : transfers) {
            pool.execute(() -> {
                try {
                    start.await();
                    strategy.post(transfer);
                    posted.incrementAndGet();
                } catch (InsufficientFundsException e) {
                    rejected.incrementAndGet();
                } catch (PostingContentionException e) {
                    gaveUp.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    unexpected.add(e);
                } catch (RuntimeException e) {
                    unexpected.add(e);
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(3, TimeUnit.MINUTES))
                .as("all transfers finished")
                .isTrue();

        System.out.printf(
                "CONCURRENCY %s: %d transfers, %d posted, %d rejected (insufficient funds), %d gave up, %.0f retries%n",
                name, TRANSFERS, posted.get(), rejected.get(), gaveUp.get(), retries(name) - retriesBefore);

        assertThat(unexpected).as("unexpected failures").isEmpty();
        assertThat(posted.get() + rejected.get() + gaveUp.get()).isEqualTo(TRANSFERS);
        assertThat(rejected.get()).as("the overdraft rule was exercised").isPositive();

        InvariantReport report = verifier.verify(books.tenant);
        assertThat(report.ok()).as("invariants: %s", report.violations()).isTrue();

        long total = 0;
        for (Account wallet : wallets) {
            long balance =
                    ledger.account(books.tenant, wallet.id()).balance().ledger().amount();
            assertThat(balance).as("wallet %s", wallet.id()).isNotNegative();
            total += balance;
        }
        assertThat(total).as("transfers only move money between the wallets").isEqualTo(ACCOUNTS * OPENING_BALANCE);

        Long entries = jdbc.sql("SELECT count(*) FROM entry WHERE tenant_id = :tenant")
                .param("tenant", books.tenant.value())
                .query(Long.class)
                .single();
        assertThat(entries)
                .as("two entries per funding and per posted transfer")
                .isEqualTo(2L * (ACCOUNTS + posted.get()));
    }

    private double retries(PostingStrategyName name) {
        return meters.get("ledger.posting.retries")
                .tag("strategy", name.name())
                .counter()
                .count();
    }
}
