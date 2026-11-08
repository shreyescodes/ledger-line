package dev.ledgerline.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.ledgerline.ledger.application.LedgerService;
import dev.ledgerline.ledger.domain.Account;
import dev.ledgerline.ledger.domain.AccountType;
import dev.ledgerline.ledger.domain.PostedTransaction;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The database enforces the ledger's invariants on its own (ADR-0013), so even a buggy or
 * malicious caller with the service's credentials can't break them, and the owner can't quietly
 * edit history either.
 */
@LedgerIntegrationTest
class LedgerSchemaIntegrationTest {

    @Autowired
    private LedgerService ledger;

    /** Connected as the service's least-privilege role. */
    @Autowired
    private JdbcTemplate app;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private PostgreSQLContainer postgres;

    private JdbcTemplate owner;
    private Books books;
    private Account alice;
    private Account bob;

    @BeforeEach
    void setUp() {
        owner = new JdbcTemplate(
                new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
        books = new Books(ledger);
        alice = books.open(AccountType.CUSTOMER_WALLET, "INR");
        bob = books.open(AccountType.CUSTOMER_WALLET, "INR");
        books.fund(alice, 10_000);
    }

    @Test
    void anUnbalancedTransactionIsRefusedAtCommit() {
        assertThatThrownBy(() -> inOneTransaction(() -> {
                    UUID tx = insertTransaction();
                    insertEntry(tx, alice, "DEBIT", 100, "INR", 1001);
                    insertEntry(tx, bob, "CREDIT", 99, "INR", 1001);
                }))
                .hasStackTraceContaining("does not balance");
    }

    @Test
    void aTransactionNeedsAtLeastTwoEntries() {
        assertThatThrownBy(() -> inOneTransaction(() -> {
                    UUID tx = insertTransaction();
                    insertEntry(tx, alice, "DEBIT", 100, "INR", 1001);
                }))
                .hasStackTraceContaining("at least 2 are required");
    }

    @Test
    void theAppRoleCanNotUpdateOrDeleteEntries() {
        PostedTransaction posted = ledger.post(books.transfer(alice, bob, 500));
        UUID entry = posted.entries().get(0).id().value();

        assertThatThrownBy(() -> app.update("UPDATE entry SET amount = 1 WHERE id = ?", entry))
                .hasStackTraceContaining("permission denied");
        assertThatThrownBy(() -> app.update("DELETE FROM entry WHERE id = ?", entry))
                .hasStackTraceContaining("permission denied");
        assertThatThrownBy(() -> app.update("DELETE FROM ledger_transaction WHERE id = ?", posted.id().value()))
                .hasStackTraceContaining("permission denied");
    }

    @Test
    void evenTheOwnerCanNotRewriteHistory() {
        PostedTransaction posted = ledger.post(books.transfer(alice, bob, 500));
        UUID entry = posted.entries().get(0).id().value();

        assertThatThrownBy(() -> owner.update("UPDATE entry SET amount = 1 WHERE id = ?", entry))
                .hasStackTraceContaining("append-only");
        assertThatThrownBy(() -> owner.update("DELETE FROM entry WHERE id = ?", entry))
                .hasStackTraceContaining("append-only");
        assertThatThrownBy(() -> owner.execute("TRUNCATE entry"))
                .hasStackTraceContaining("append-only");
    }

    @Test
    void anEntryMustBeInItsAccountsCurrency() {
        assertThatThrownBy(() -> inOneTransaction(() -> {
                    UUID tx = insertTransaction();
                    insertEntry(tx, alice, "DEBIT", 100, "USD", 1001);
                    insertEntry(tx, bob, "CREDIT", 100, "USD", 1001);
                }))
                .hasStackTraceContaining("foreign key");
    }

    @Test
    void theDatabaseItselfRefusesAnOverdraft() {
        // A wallet is credit-normal: a positive raw (debit) balance would mean Acme is owed money.
        assertThatThrownBy(() -> app.update(
                        "UPDATE account_balance SET raw_balance = 1 WHERE account_id = ?", alice.id().value()))
                .hasStackTraceContaining("balance_not_overdrawn");
    }

    @Test
    void theAppRoleCanNotChangeAnAccountsFlags() {
        assertThatThrownBy(() -> app.update(
                        "UPDATE account_balance SET non_negative = false WHERE account_id = ?", alice.id().value()))
                .hasStackTraceContaining("permission denied");
    }

    @Test
    void theAppRoleCanStillPostThroughTheLedger() {
        PostedTransaction posted = ledger.post(books.transfer(alice, bob, 500));

        assertThat(posted.entries()).hasSize(2);
        assertThat(ledger.account(books.tenant, bob.id()).balance().ledger().amount()).isEqualTo(500);
    }

    private void inOneTransaction(Runnable work) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> work.run());
    }

    private UUID insertTransaction() {
        UUID id = UUID.randomUUID();
        app.update(
                "INSERT INTO ledger_transaction (id, tenant_id, type, recorded_at) VALUES (?, ?, 'ADJUSTMENT', ?)",
                id,
                books.tenant.value(),
                Timestamp.from(Instant.parse("2026-11-02T10:00:00Z")));
        return id;
    }

    private void insertEntry(UUID tx, Account account, String side, long amount, String currency, long seq) {
        app.update(
                "INSERT INTO entry (id, tenant_id, transaction_id, account_id, currency, side, amount, account_seq)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(),
                books.tenant.value(),
                tx,
                account.id().value(),
                currency,
                side,
                amount,
                seq);
    }
}
