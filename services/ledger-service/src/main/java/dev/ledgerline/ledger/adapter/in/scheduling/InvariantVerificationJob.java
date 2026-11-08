package dev.ledgerline.ledger.adapter.in.scheduling;

import dev.ledgerline.ledger.application.InvariantReport;
import dev.ledgerline.ledger.application.InvariantVerifier;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the invariant verifier on a schedule and publishes the result as metrics:
 * {@code ledger_invariant_violations{check=...}} (should always be 0) and
 * {@code ledger_invariant_last_verified_seconds}. Stage 8 alerts on both.
 */
@Component
@ConditionalOnProperty(prefix = "ledger.verifier", name = "enabled", havingValue = "true", matchIfMissing = true)
class InvariantVerificationJob {

    private static final Logger LOG = LoggerFactory.getLogger(InvariantVerificationJob.class);
    private static final List<String> CHECKS = List.of(
            InvariantReport.BALANCES_MATCH_ENTRIES,
            InvariantReport.BOOKS_SUM_TO_ZERO,
            InvariantReport.NO_OVERDRAWN_ACCOUNTS,
            InvariantReport.TRANSACTIONS_BALANCE,
            InvariantReport.SEQUENCES_MATCH_VERSIONS);

    private final InvariantVerifier verifier;
    private final Map<String, AtomicLong> violations = new ConcurrentHashMap<>();
    private final AtomicLong lastVerifiedEpochSecond = new AtomicLong();

    InvariantVerificationJob(InvariantVerifier verifier, MeterRegistry meters) {
        this.verifier = verifier;
        for (String check : CHECKS) {
            AtomicLong count = new AtomicLong();
            violations.put(check, count);
            Gauge.builder("ledger.invariant.violations", count, AtomicLong::get)
                    .description("Violations found by the last invariant check (should be 0)")
                    .tag("check", check)
                    .register(meters);
        }
        Gauge.builder("ledger.invariant.last.verified", lastVerifiedEpochSecond, AtomicLong::get)
                .description("When the invariants were last verified, in epoch seconds")
                .baseUnit("seconds")
                .register(meters);
    }

    @Scheduled(fixedDelayString = "${ledger.verifier.interval:PT1M}", initialDelayString = "PT10S")
    void verify() {
        InvariantReport report = verifier.verifyAll();
        report.violations().forEach((check, count) -> violations
                .computeIfAbsent(check, name -> new AtomicLong())
                .set(count));
        lastVerifiedEpochSecond.set(report.checkedAt().getEpochSecond());
        if (!report.ok()) {
            LOG.error("Ledger invariants violated: {}", report.violations());
        }
    }
}
