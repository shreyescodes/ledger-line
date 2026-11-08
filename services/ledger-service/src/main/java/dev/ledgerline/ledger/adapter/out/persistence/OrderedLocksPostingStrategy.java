package dev.ledgerline.ledger.adapter.out.persistence;

import dev.ledgerline.ledger.application.PostingOptions;
import dev.ledgerline.ledger.application.PostingStrategy;
import dev.ledgerline.ledger.application.PostingStrategyName;
import dev.ledgerline.ledger.domain.AccountBalance;
import dev.ledgerline.ledger.domain.AccountId;
import dev.ledgerline.ledger.domain.PostedTransaction;
import dev.ledgerline.ledger.domain.PostingPlan;
import dev.ledgerline.ledger.domain.PostingRules;
import dev.ledgerline.ledger.domain.TransactionDraft;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Pessimistic: lock every touched balance row ({@code SELECT ... FOR UPDATE}) in account-id order,
 * check the rules against the locked values, then write. Concurrent postings to the same account
 * queue on the row lock; the fixed lock order means they can't deadlock. Retries only cover the
 * rare transient failure, such as a connection-level deadlock report.
 */
@Component
class OrderedLocksPostingStrategy implements PostingStrategy {

    private final PostingSql sql;
    private final TransactionTemplate transactions;
    private final ConcurrencyRetry retry;

    OrderedLocksPostingStrategy(
            PostingSql sql, PlatformTransactionManager transactionManager, PostingOptions options, MeterRegistry meters) {
        this.sql = sql;
        this.transactions = new TransactionTemplate(transactionManager);
        this.transactions.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        this.retry = new ConcurrencyRetry(name(), options.maxAttempts(), meters);
    }

    @Override
    public PostingStrategyName name() {
        return PostingStrategyName.ORDERED_LOCKS;
    }

    @Override
    public PostedTransaction post(TransactionDraft draft) {
        PostingSql.Identity identity = sql.newIdentity(draft);
        return retry.run(() -> transactions.execute(status -> {
            Map<AccountId, AccountBalance> locked = sql.balances(draft, true);
            PostingPlan plan = PostingRules.plan(draft, locked);
            PostedTransaction posted = sql.insert(draft, plan, identity);
            sql.updateBalances(plan, identity);
            return posted;
        }));
    }
}
