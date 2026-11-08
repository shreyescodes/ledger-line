package dev.ledgerline.ledger.application;

import dev.ledgerline.common.money.Money;
import dev.ledgerline.ledger.domain.EntryId;
import dev.ledgerline.ledger.domain.Side;
import dev.ledgerline.ledger.domain.TransactionId;
import dev.ledgerline.ledger.domain.TransactionType;
import java.time.Instant;

/** One line of an account statement: an entry plus a little about its transaction. */
public record StatementLine(
        EntryId entryId,
        TransactionId transactionId,
        TransactionType transactionType,
        String reference,
        Instant recordedAt,
        Side side,
        Money amount,
        long accountSeq) {}
