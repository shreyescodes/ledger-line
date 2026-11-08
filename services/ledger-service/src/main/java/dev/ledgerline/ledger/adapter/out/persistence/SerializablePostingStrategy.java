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
 * Serializable: plain reads and writes, but at SERIALIZABLE isolation. Postgres detects any
 * interleaving that couldn't have happened one-at-a-time and aborts one side with 40001; that
 * side is retried from the start.
 */
@Component
class SerializablePostingStrategy implements PostingStrategy {

    private final PostingSql sql;
    private final TransactionTemplate transactions;
    private final ConcurrencyRetry retry;

    SerializablePostingStrategy(
            PostingSql sql,
            PlatformTransactionManager transactionManager,
            PostingOptions options,
            MeterRegistry meters) {
        this.sql = sql;
        this.transactions = new TransactionTemplate(transactionManager);
        this.transactions.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        this.retry = new ConcurrencyRetry(name(), options.maxAttempts(), meters);
    }

    @Override
    public PostingStrategyName name() {
        return PostingStrategyName.SERIALIZABLE;
    }

    @Override
    public PostedTransaction post(TransactionDraft draft) {
        PostingSql.Identity identity = sql.newIdentity(draft);
        return retry.run(() -> transactions.execute(status -> {
            Map<AccountId, AccountBalance> read = sql.balances(draft, false);
            PostingPlan plan = PostingRules.plan(draft, read);
            PostedTransaction posted = sql.insert(draft, plan, identity);
            sql.updateBalances(plan, identity);
            return posted;
        }));
    }
}
