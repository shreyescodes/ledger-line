package dev.ledgerline.ledger.application;

import dev.ledgerline.ledger.domain.PostedTransaction;
import dev.ledgerline.ledger.domain.TransactionDraft;

/**
 * Posts a transaction atomically: entries, transaction row and balance projection in one database
 * transaction, without ever letting a non-negative account go below zero under concurrency.
 */
public interface PostingStrategy {

    PostingStrategyName name();

    /**
     * Posts the draft, or refuses it and writes nothing.
     *
     * @throws dev.ledgerline.ledger.domain.LedgerException when the ledger refuses the posting
     * @throws PostingContentionException when retries ran out under contention
     */
    PostedTransaction post(TransactionDraft draft);
}
