package dev.ledgerline.ledger.domain;

/** Why a ledger transaction exists (DOMAIN.md §5). */
public enum TransactionType {
    TRANSFER,
    PAYMENT,
    TOP_UP,
    REFUND,
    PAYOUT,
    CHARGEBACK,
    DISPUTE_FEE,
    SETTLEMENT,
    ADJUSTMENT,
    REVERSAL
}
