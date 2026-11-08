package dev.ledgerline.ledger.adapter.out.persistence;

import dev.ledgerline.common.money.Money;
import dev.ledgerline.ledger.application.StatementLine;
import dev.ledgerline.ledger.application.StatementPage;
import dev.ledgerline.ledger.application.StatementQuery;
import dev.ledgerline.ledger.domain.AccountId;
import dev.ledgerline.ledger.domain.EntryId;
import dev.ledgerline.ledger.domain.Side;
import dev.ledgerline.ledger.domain.TenantId;
import dev.ledgerline.ledger.domain.TransactionId;
import dev.ledgerline.ledger.domain.TransactionType;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcStatementQuery implements StatementQuery {

    private final JdbcClient jdbc;

    JdbcStatementQuery(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public StatementPage page(TenantId tenant, AccountId account, long afterSeq, int limit) {
        // One row more than asked for tells us whether another page exists.
        List<StatementLine> rows = jdbc.sql("""
                        SELECT e.id, e.transaction_id, t.type, t.reference, t.recorded_at,
                               e.side, e.amount, e.currency, e.account_seq
                        FROM entry e
                        JOIN ledger_transaction t ON t.id = e.transaction_id
                        WHERE e.tenant_id = :tenant AND e.account_id = :account AND e.account_seq > :after
                        ORDER BY e.account_seq
                        LIMIT :limit
                        """)
                .param("tenant", tenant.value())
                .param("account", account.value())
                .param("after", afterSeq)
                .param("limit", limit + 1)
                .query((rs, n) -> new StatementLine(
                        new EntryId(rs.getObject("id", UUID.class)),
                        new TransactionId(rs.getObject("transaction_id", UUID.class)),
                        TransactionType.valueOf(rs.getString("type")),
                        rs.getString("reference"),
                        rs.getTimestamp("recorded_at").toInstant(),
                        Side.valueOf(rs.getString("side")),
                        Money.of(rs.getLong("amount"), rs.getString("currency")),
                        rs.getLong("account_seq")))
                .list();
        if (rows.size() <= limit) {
            return new StatementPage(rows, OptionalLong.empty());
        }
        List<StatementLine> page = rows.subList(0, limit);
        return new StatementPage(page, OptionalLong.of(page.get(limit - 1).accountSeq()));
    }
}
