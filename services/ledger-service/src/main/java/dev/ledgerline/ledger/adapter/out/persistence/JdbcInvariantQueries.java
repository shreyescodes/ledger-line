package dev.ledgerline.ledger.adapter.out.persistence;

import dev.ledgerline.ledger.application.InvariantQueries;
import dev.ledgerline.ledger.application.InvariantReport;
import dev.ledgerline.ledger.domain.TenantId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The verifier's checks as SQL. They run in one REPEATABLE READ, read-only transaction, so all
 * checks see the same snapshot even while postings continue.
 */
@Repository
class JdbcInvariantQueries implements InvariantQueries {

    private static final String SIGNED = "CASE e.side WHEN 'DEBIT' THEN e.amount ELSE -e.amount END";

    private static final Map<String, String> CHECKS = checks();

    private final JdbcClient jdbc;
    private final TransactionTemplate snapshot;

    JdbcInvariantQueries(JdbcClient jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.snapshot = new TransactionTemplate(transactionManager);
        this.snapshot.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        this.snapshot.setReadOnly(true);
    }

    @Override
    public Map<String, Long> countViolations(Optional<TenantId> tenant) {
        UUID tenantId = tenant.map(TenantId::value).orElse(null);
        Map<String, Long> counts = snapshot.execute(status -> {
            Map<String, Long> result = new LinkedHashMap<>();
            CHECKS.forEach((name, sql) -> result.put(
                    name,
                    jdbc.sql(sql).param("tenant", tenantId).query(Long.class).single()));
            return result;
        });
        return counts == null ? Map.of() : counts;
    }

    // Each query counts violations. ":tenant" is null to check every tenant.
    private static Map<String, String> checks() {
        Map<String, String> checks = new LinkedHashMap<>();
        // I4: the stored balance equals the sum of the account's entries.
        checks.put(InvariantReport.BALANCES_MATCH_ENTRIES, """
                SELECT count(*) FROM account_balance b
                LEFT JOIN (SELECT e.account_id, sum(%s) AS replayed FROM entry e GROUP BY e.account_id) r
                       ON r.account_id = b.account_id
                WHERE (CAST(:tenant AS uuid) IS NULL OR b.tenant_id = :tenant)
                  AND b.raw_balance <> COALESCE(r.replayed, 0)
                """.formatted(SIGNED));
        // I12: per tenant and currency, raw balances sum to zero.
        checks.put(InvariantReport.BOOKS_SUM_TO_ZERO, """
                SELECT count(*) FROM (
                    SELECT b.tenant_id, b.currency FROM account_balance b
                    WHERE (CAST(:tenant AS uuid) IS NULL OR b.tenant_id = :tenant)
                    GROUP BY b.tenant_id, b.currency
                    HAVING sum(b.raw_balance) <> 0) unbalanced
                """);
        // I5: no non-negative account below zero.
        checks.put(InvariantReport.NO_OVERDRAWN_ACCOUNTS, """
                SELECT count(*) FROM account_balance b
                WHERE (CAST(:tenant AS uuid) IS NULL OR b.tenant_id = :tenant)
                  AND b.non_negative AND b.raw_balance * b.normal_sign < 0
                """);
        // I2: every transaction balances per currency.
        checks.put(InvariantReport.TRANSACTIONS_BALANCE, """
                SELECT count(*) FROM (
                    SELECT e.transaction_id FROM entry e
                    WHERE (CAST(:tenant AS uuid) IS NULL OR e.tenant_id = :tenant)
                    GROUP BY e.transaction_id, e.currency
                    HAVING sum(%s) <> 0) unbalanced
                """.formatted(SIGNED));
        // Each entry bumps its account's version by one, so version == number of entries == max seq.
        checks.put(InvariantReport.SEQUENCES_MATCH_VERSIONS, """
                SELECT count(*) FROM account_balance b
                LEFT JOIN (SELECT e.account_id, count(*) AS entries, max(e.account_seq) AS last_seq
                           FROM entry e GROUP BY e.account_id) s
                       ON s.account_id = b.account_id
                WHERE (CAST(:tenant AS uuid) IS NULL OR b.tenant_id = :tenant)
                  AND (b.version <> COALESCE(s.entries, 0) OR b.version <> COALESCE(s.last_seq, 0))
                """);
        return Map.copyOf(checks);
    }
}
