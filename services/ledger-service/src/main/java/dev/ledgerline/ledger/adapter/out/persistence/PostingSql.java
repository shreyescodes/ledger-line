package dev.ledgerline.ledger.adapter.out.persistence;

import dev.ledgerline.common.id.IdGenerator;
import dev.ledgerline.common.money.CurrencyCode;
import dev.ledgerline.common.money.Money;
import dev.ledgerline.ledger.domain.AccountBalance;
import dev.ledgerline.ledger.domain.AccountId;
import dev.ledgerline.ledger.domain.AccountType;
import dev.ledgerline.ledger.domain.EntryDraft;
import dev.ledgerline.ledger.domain.EntryId;
import dev.ledgerline.ledger.domain.PostedEntry;
import dev.ledgerline.ledger.domain.PostedTransaction;
import dev.ledgerline.ledger.domain.PostingPlan;
import dev.ledgerline.ledger.domain.TenantId;
import dev.ledgerline.ledger.domain.TransactionDraft;
import dev.ledgerline.ledger.domain.TransactionId;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** The SQL all three posting strategies share. Callers own the database transaction. */
@Component
class PostingSql {

    private static final String SELECT_BALANCES = """
            SELECT b.account_id, b.tenant_id, a.type, b.currency, b.raw_balance, b.version
            FROM account_balance b
            JOIN account a ON a.id = b.account_id
            WHERE b.tenant_id = :tenant AND b.account_id IN (:ids)
            ORDER BY b.account_id
            """;

    private final JdbcClient jdbc;
    private final JdbcTemplate template;
    private final IdGenerator ids;
    private final Clock clock;

    PostingSql(JdbcClient jdbc, JdbcTemplate template, IdGenerator ids, Clock clock) {
        this.jdbc = jdbc;
        this.template = template;
        this.ids = ids;
        this.clock = clock;
    }

    /** Ids and timestamp for one posting, made once so a retried attempt writes the same values. */
    Identity newIdentity(TransactionDraft draft) {
        List<EntryId> entryIds = new ArrayList<>();
        for (int i = 0; i < draft.entries().size(); i++) {
            entryIds.add(new EntryId(ids.next()));
        }
        return new Identity(new TransactionId(ids.next()), entryIds, Instant.now(clock));
    }

    /**
     * Reads the balances a draft touches, in account-id order. With {@code lock}, each row is
     * locked as it is read; because the order is always the same, two postings can't deadlock.
     */
    Map<AccountId, AccountBalance> balances(TransactionDraft draft, boolean lock) {
        List<UUID> accountIds = new ArrayList<>(new TreeSet<>(
                draft.entries().stream().map(e -> e.account().value()).toList()));
        String sql = lock ? SELECT_BALANCES + "FOR UPDATE OF b" : SELECT_BALANCES;
        List<AccountBalance> rows = jdbc.sql(sql)
                .param("tenant", draft.tenantId().value())
                .param("ids", accountIds)
                .query((rs, n) -> new AccountBalance(
                        new AccountId(rs.getObject("account_id", UUID.class)),
                        new TenantId(rs.getObject("tenant_id", UUID.class)),
                        AccountType.valueOf(rs.getString("type")),
                        CurrencyCode.of(rs.getString("currency")),
                        rs.getLong("raw_balance"),
                        rs.getLong("version")))
                .list();
        Map<AccountId, AccountBalance> byId = new HashMap<>();
        rows.forEach(balance -> byId.put(balance.accountId(), balance));
        return byId;
    }

    /** Inserts the transaction row and its entries. */
    PostedTransaction insert(TransactionDraft draft, PostingPlan plan, Identity identity) {
        Timestamp recordedAt = Timestamp.from(identity.recordedAt());
        template.update(
                "INSERT INTO ledger_transaction (id, tenant_id, type, reference, recorded_at) VALUES (?, ?, ?, ?, ?)",
                identity.transactionId().value(),
                draft.tenantId().value(),
                draft.type().name(),
                draft.reference(),
                recordedAt);
        List<Object[]> rows = new ArrayList<>();
        List<PostedEntry> posted = new ArrayList<>();
        for (int i = 0; i < plan.entries().size(); i++) {
            PostingPlan.PlannedEntry planned = plan.entries().get(i);
            EntryDraft entry = planned.draft();
            EntryId entryId = identity.entryIds().get(i);
            Money amount = entry.amount();
            rows.add(new Object[] {
                entryId.value(),
                draft.tenantId().value(),
                identity.transactionId().value(),
                entry.account().value(),
                amount.currency().code(),
                entry.side().name(),
                amount.amount(),
                planned.accountSeq()
            });
            posted.add(new PostedEntry(entryId, entry.account(), entry.side(), amount, planned.accountSeq()));
        }
        template.batchUpdate(
                "INSERT INTO entry (id, tenant_id, transaction_id, account_id, currency, side, amount, account_seq)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                rows);
        return new PostedTransaction(
                identity.transactionId(),
                draft.tenantId(),
                draft.type(),
                draft.reference(),
                identity.recordedAt(),
                posted);
    }

    /** Writes new balances without conditions: the caller already holds the rows' locks. */
    void updateBalances(PostingPlan plan, Identity identity) {
        Timestamp updatedAt = Timestamp.from(identity.recordedAt());
        List<Object[]> rows = new ArrayList<>();
        for (AccountBalance balance : plan.newBalances().values()) {
            rows.add(new Object[] {balance.raw(), balance.version(), updatedAt, balance.accountId().value()});
        }
        template.batchUpdate(
                "UPDATE account_balance SET raw_balance = ?, version = ?, updated_at = ? WHERE account_id = ?", rows);
    }

    /**
     * Writes a new balance only if nobody changed the row since {@code expectedVersion} was read.
     *
     * @return false if another posting got there first
     */
    boolean updateBalanceIfUnchanged(AccountBalance balance, long expectedVersion, Identity identity) {
        int updated = template.update(
                "UPDATE account_balance SET raw_balance = ?, version = ?, updated_at = ?"
                        + " WHERE account_id = ? AND version = ?",
                balance.raw(),
                balance.version(),
                Timestamp.from(identity.recordedAt()),
                balance.accountId().value(),
                expectedVersion);
        return updated == 1;
    }

    record Identity(TransactionId transactionId, List<EntryId> entryIds, Instant recordedAt) {}
}
