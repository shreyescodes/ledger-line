package dev.ledgerline.ledger.adapter.out.persistence;

import dev.ledgerline.common.money.CurrencyCode;
import dev.ledgerline.ledger.application.AccountStore;
import dev.ledgerline.ledger.domain.Account;
import dev.ledgerline.ledger.domain.AccountBalance;
import dev.ledgerline.ledger.domain.AccountId;
import dev.ledgerline.ledger.domain.AccountType;
import dev.ledgerline.ledger.domain.TenantId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JdbcAccountStore implements AccountStore {

    private static final String SELECT_ACCOUNT =
            "SELECT id, tenant_id, type, currency, holder_ref, created_at FROM account WHERE tenant_id = :tenant ";

    private final JdbcClient jdbc;

    JdbcAccountStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void create(Account account) {
        Timestamp createdAt = Timestamp.from(account.createdAt());
        jdbc.sql("""
                        INSERT INTO account (id, tenant_id, type, currency, holder_ref, created_at)
                        VALUES (:id, :tenant, :type, :currency, :holderRef, :createdAt)
                        """)
                .param("id", account.id().value())
                .param("tenant", account.tenantId().value())
                .param("type", account.type().name())
                .param("currency", account.currency().code())
                .param("holderRef", account.holderRef())
                .param("createdAt", createdAt)
                .update();
        jdbc.sql("""
                        INSERT INTO account_balance
                            (account_id, tenant_id, currency, normal_sign, non_negative, raw_balance, version, updated_at)
                        VALUES (:id, :tenant, :currency, :normalSign, :nonNegative, 0, 0, :updatedAt)
                        """)
                .param("id", account.id().value())
                .param("tenant", account.tenantId().value())
                .param("currency", account.currency().code())
                .param("normalSign", (short) account.type().normalSign())
                .param("nonNegative", account.type().nonNegative())
                .param("updatedAt", createdAt)
                .update();
    }

    @Override
    public Optional<Account> find(TenantId tenant, AccountId account) {
        return jdbc.sql(SELECT_ACCOUNT + "AND id = :id")
                .param("tenant", tenant.value())
                .param("id", account.value())
                .query(JdbcAccountStore::account)
                .optional();
    }

    @Override
    public List<Account> findAll(TenantId tenant, Collection<AccountId> accounts) {
        if (accounts.isEmpty()) {
            return List.of();
        }
        return jdbc.sql(SELECT_ACCOUNT + "AND id IN (:ids)")
                .param("tenant", tenant.value())
                .param("ids", accounts.stream().map(AccountId::value).toList())
                .query(JdbcAccountStore::account)
                .list();
    }

    @Override
    public Optional<AccountBalance> balance(TenantId tenant, AccountId account) {
        return jdbc.sql("""
                        SELECT b.account_id, b.tenant_id, a.type, b.currency, b.raw_balance, b.version
                        FROM account_balance b
                        JOIN account a ON a.id = b.account_id
                        WHERE b.tenant_id = :tenant AND b.account_id = :id
                        """)
                .param("tenant", tenant.value())
                .param("id", account.value())
                .query((rs, n) -> new AccountBalance(
                        new AccountId(rs.getObject("account_id", UUID.class)),
                        new TenantId(rs.getObject("tenant_id", UUID.class)),
                        AccountType.valueOf(rs.getString("type")),
                        CurrencyCode.of(rs.getString("currency")),
                        rs.getLong("raw_balance"),
                        rs.getLong("version")))
                .optional();
    }

    private static Account account(ResultSet rs, int rowNum) throws SQLException {
        return new Account(
                new AccountId(rs.getObject("id", UUID.class)),
                new TenantId(rs.getObject("tenant_id", UUID.class)),
                AccountType.valueOf(rs.getString("type")),
                CurrencyCode.of(rs.getString("currency")),
                rs.getString("holder_ref"),
                rs.getTimestamp("created_at").toInstant());
    }
}
