package dev.ledgerline.ledger.application;

import dev.ledgerline.ledger.domain.AccountId;
import dev.ledgerline.ledger.domain.TenantId;

/**
 * Reads an account's entries in order. Pages by the account sequence number (keyset pagination),
 * which is gap-free and assigned under the balance row's lock, so no entry is ever skipped.
 */
public interface StatementQuery {

    StatementPage page(TenantId tenant, AccountId account, long afterSeq, int limit);
}
