package dev.ledgerline.ledger.domain;

import dev.ledgerline.common.money.Money;

/** An entry as written: immutable, with its sequence number on the account. */
public record PostedEntry(EntryId id, AccountId account, Side side, Money amount, long accountSeq) {}
