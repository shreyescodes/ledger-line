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
import dev.ledgerline.testsupport.containers.PostgresWithAppRoleConfiguration;
import dev.ledgerline.testsupport.docker.RequiresDocker;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Compares the three overdraft-prevention strategies (ADR-0014). Not part of {@code test}: run it
 * with {@code ./gradlew :services:ledger-service:benchmark}. Raw results go to build/benchmark;
 * numbers only count once that output is saved under docs/results with the machine it ran on.
 *
 * <p>Two scenarios, each run {@value #REPETITIONS} times per strategy after a warm-up, every run
 * in a fresh tenant: HOT (all transfers among {@value #HOT_ACCOUNTS} accounts, so they fight over
 * the same rows) and SPREAD ({@value #SPREAD_ACCOUNTS} accounts, little contention). Accounts are
 * funded generously, so the work measured is posting, not refusing.
 */
@Tag("benchmark")
@RequiresDocker
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "ledger.verifier.enabled=false",
            "spring.datasource.hikari.maximum-pool-size=" + PostingStrategyBenchmark.WORKERS
        })
@Import(PostgresWithAppRoleConfiguration.class)
class PostingStrategyBenchmark {

    static final int WORKERS = 16;
    private static final int TRANSFERS = 4_000;
    private static final int WARMUP_TRANSFERS = 500;
    private static final int REPETITIONS = 3;
    private static final int HOT_ACCOUNTS = 10;
    private static final int SPREAD_ACCOUNTS = 1_000;
    private static final long OPENING_BALANCE = 1_000_000_000L;

    @Autowired
    private LedgerService ledger;

    @Autowired
    private List<PostingStrategy> strategies;

    @Autowired
    private InvariantVerifier verifier;

    @Autowired
    private MeterRegistry meters;

    @Autowired
    private JdbcClient jdbc;

    @Value("${benchmark.output:build/benchmark}")
    private Path output;

    private record Run(
            String strategy,
            String scenario,
            int repetition,
            int transfers,
            int posted,
            int rejected,
            int gaveUp,
            long retries,
            double seconds,
            double throughput,
            double p50,
            double p95,
            double p99,
            double max) {}

    @Test
    void compareTheStrategies() throws Exception {
        List<Run> runs = new ArrayList<>();
        for (PostingStrategy strategy : strategies.stream()
                .sorted(Comparator.comparing(PostingStrategy::name))
                .toList()) {
            for (int accounts : new int[] {HOT_ACCOUNTS, SPREAD_ACCOUNTS}) {
                String scenario = accounts == HOT_ACCOUNTS ? "HOT" : "SPREAD";
                run(strategy, scenario, accounts, WARMUP_TRANSFERS, 0);
                for (int repetition = 1; repetition <= REPETITIONS; repetition++) {
                    Run run = run(strategy, scenario, accounts, TRANSFERS, repetition);
                    System.out.println("BENCHMARK " + run);
                    runs.add(run);
                }
            }
        }
        write(runs);
    }

    private Run run(PostingStrategy strategy, String scenario, int accountCount, int transferCount, int repetition)
            throws InterruptedException {
        Books books = new Books(ledger);
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < accountCount; i++) {
            Account account = books.open(AccountType.CUSTOMER_WALLET, "INR");
            books.fund(account, OPENING_BALANCE, ledger::post);
            accounts.add(account);
        }
        Random random = new Random(1_000L * repetition + accountCount);
        List<TransactionDraft> transfers = new ArrayList<>();
        for (int i = 0; i < transferCount; i++) {
            int from = random.nextInt(accountCount);
            int to = (from + 1 + random.nextInt(accountCount - 1)) % accountCount;
            transfers.add(books.transfer(accounts.get(from), accounts.get(to), 1 + random.nextInt(1_000)));
        }
        long[] latencies = new long[transferCount];
        AtomicInteger posted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        AtomicInteger gaveUp = new AtomicInteger();
        AtomicInteger next = new AtomicInteger();
        double retriesBefore = retries(strategy.name());
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(WORKERS);
        for (int worker = 0; worker < WORKERS; worker++) {
            pool.execute(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int i = next.getAndIncrement(); i < transferCount; i = next.getAndIncrement()) {
                    long began = System.nanoTime();
                    try {
                        strategy.post(transfers.get(i));
                        posted.incrementAndGet();
                    } catch (InsufficientFundsException e) {
                        rejected.incrementAndGet();
                    } catch (PostingContentionException e) {
                        gaveUp.incrementAndGet();
                    }
                    latencies[i] = System.nanoTime() - began;
                }
            });
        }
        long began = System.nanoTime();
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.MINUTES)).isTrue();
        double seconds = (System.nanoTime() - began) / 1e9;

        InvariantReport report = verifier.verify(books.tenant);
        assertThat(report.ok())
                .as("invariants after %s %s: %s", strategy.name(), scenario, report.violations())
                .isTrue();
        assertThat(posted.get() + rejected.get() + gaveUp.get()).isEqualTo(transferCount);

        long[] sorted = latencies.clone();
        Arrays.sort(sorted);
        return new Run(
                strategy.name().name(),
                scenario,
                repetition,
                transferCount,
                posted.get(),
                rejected.get(),
                gaveUp.get(),
                Math.round(retries(strategy.name()) - retriesBefore),
                seconds,
                transferCount / seconds,
                millis(percentile(sorted, 0.50)),
                millis(percentile(sorted, 0.95)),
                millis(percentile(sorted, 0.99)),
                millis(sorted[sorted.length - 1]));
    }

    private void write(List<Run> runs) throws IOException {
        Files.createDirectories(output);
        StringBuilder lines = new StringBuilder();
        for (Run run : runs) {
            lines.append(String.format(
                    Locale.ROOT,
                    "{\"strategy\":\"%s\",\"scenario\":\"%s\",\"repetition\":%d,\"transfers\":%d,\"posted\":%d,"
                            + "\"rejected\":%d,\"gaveUp\":%d,\"retries\":%d,\"seconds\":%.3f,\"throughput\":%.1f,"
                            + "\"p50Ms\":%.3f,\"p95Ms\":%.3f,\"p99Ms\":%.3f,\"maxMs\":%.3f}%n",
                    run.strategy(),
                    run.scenario(),
                    run.repetition(),
                    run.transfers(),
                    run.posted(),
                    run.rejected(),
                    run.gaveUp(),
                    run.retries(),
                    run.seconds(),
                    run.throughput(),
                    run.p50(),
                    run.p95(),
                    run.p99(),
                    run.max()));
        }
        Files.writeString(output.resolve("posting-strategies.jsonl"), lines, StandardCharsets.UTF_8);

        String postgres = jdbc.sql("SELECT version()").query(String.class).single();
        StringBuilder table = new StringBuilder();
        table.append("# Posting strategy benchmark (raw output)\n\n")
                .append(String.format(
                        Locale.ROOT,
                        "- Java: %s %s%n",
                        System.getProperty("java.vendor"),
                        System.getProperty("java.version")))
                .append(String.format(
                        Locale.ROOT,
                        "- Processors visible to the JVM: %d%n",
                        Runtime.getRuntime().availableProcessors()))
                .append("- Postgres: ")
                .append(postgres)
                .append('\n')
                .append(String.format(
                        Locale.ROOT,
                        "- Workers: %d threads, connection pool %d; %d transfers per run, %d runs per cell after a %d-transfer warm-up%n%n",
                        WORKERS,
                        WORKERS,
                        TRANSFERS,
                        REPETITIONS,
                        WARMUP_TRANSFERS))
                .append("Median of the runs in each cell:\n\n")
                .append(
                        "| Strategy | Scenario | Throughput (transfers/s) | p50 ms | p95 ms | p99 ms | max ms | Retries | Gave up |\n")
                .append("|---|---|---:|---:|---:|---:|---:|---:|---:|\n");
        for (PostingStrategyName strategy : PostingStrategyName.values()) {
            for (String scenario : List.of("HOT", "SPREAD")) {
                List<Run> cell = runs.stream()
                        .filter(r -> r.strategy().equals(strategy.name())
                                && r.scenario().equals(scenario))
                        .toList();
                if (cell.isEmpty()) {
                    continue;
                }
                table.append(String.format(
                        Locale.ROOT,
                        "| %s | %s | %.0f | %.2f | %.2f | %.2f | %.2f | %.0f | %.0f |%n",
                        strategy,
                        scenario,
                        median(cell, Run::throughput),
                        median(cell, Run::p50),
                        median(cell, Run::p95),
                        median(cell, Run::p99),
                        median(cell, Run::max),
                        median(cell, r -> (double) r.retries()),
                        median(cell, r -> (double) r.gaveUp())));
            }
        }
        Files.writeString(output.resolve("posting-strategies.md"), table, StandardCharsets.UTF_8);
        System.out.println(table);
    }

    private double retries(PostingStrategyName name) {
        return meters.get("ledger.posting.retries")
                .tag("strategy", name.name())
                .counter()
                .count();
    }

    private static long percentile(long[] sorted, double quantile) {
        int index = (int) Math.ceil(quantile * sorted.length) - 1;
        return sorted[Math.clamp(index, 0, sorted.length - 1)];
    }

    private static double millis(long nanos) {
        return nanos / 1e6;
    }

    private static double median(List<Run> runs, java.util.function.ToDoubleFunction<Run> metric) {
        double[] values = runs.stream().mapToDouble(metric).sorted().toArray();
        int middle = values.length / 2;
        return values.length % 2 == 1 ? values[middle] : (values[middle - 1] + values[middle]) / 2;
    }
}
