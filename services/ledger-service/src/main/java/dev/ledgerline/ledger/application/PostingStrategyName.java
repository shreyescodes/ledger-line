package dev.ledgerline.ledger.application;

/** The three ways the ledger can keep non-negative accounts from going below zero (ADR-0014). */
public enum PostingStrategyName {
    /** SELECT ... FOR UPDATE on the touched balances, always in account-id order. */
    ORDERED_LOCKS,
    /** Read without locks, then update only if each balance's version is unchanged; retry if not. */
    OPTIMISTIC,
    /** Run the posting at SERIALIZABLE isolation and retry when Postgres reports a conflict. */
    SERIALIZABLE
}
