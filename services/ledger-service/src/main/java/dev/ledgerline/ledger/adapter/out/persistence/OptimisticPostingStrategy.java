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
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Optimistic: read balances without locking, check the rules, then update each balance only if
 * its version is still the one we read ({@code UPDATE ... WHERE version = ?}). If any row changed
 * underneath us, the whole database transaction rolls back and the posting is retried.
 */
@Component
class OptimisticPostingStrategy implements PostingStrategy {

    private final PostingSql sql;
    private final TransactionTemplate transactions;
    private final ConcurrencyRetry retry;

    OptimisticPostingStrategy(
            PostingSql sql,
            PlatformTransactionManager transactionManager,
            PostingOptions options,
            MeterRegistry meters) {
        this.sql = sql;
        this.transactions = new TransactionTemplate(transactionManager);
        this.transactions.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        this.retry = new ConcurrencyRetry(name(), options.maxAttempts(), meters);
    }

    @Override
    public PostingStrategyName name() {
        return PostingStrategyName.OPTIMISTIC;
    }

    @Override
    public PostedTransaction post(TransactionDraft draft) {
        PostingSql.Identity identity = sql.newIdentity(draft);
        return retry.run(() -> transactions.execute(status -> {
            Map<AccountId, AccountBalance> read = sql.balances(draft, false);
            PostingPlan plan = PostingRules.plan(draft, read);
            PostedTransaction posted = sql.insert(draft, plan, identity);
            // Updated in account-id order, like the lock strategy, so writers can't deadlock.
            for (AccountBalance balance : plan.newBalances().values()) {
                long expected = read.get(balance.accountId()).version();
                if (!sql.updateBalanceIfUnchanged(balance, expected, identity)) {
                    throw new OptimisticLockingFailureException(
                            "balance of " + balance.accountId() + " changed after it was read");
                }
            }
            return posted;
        }));
    }
}
