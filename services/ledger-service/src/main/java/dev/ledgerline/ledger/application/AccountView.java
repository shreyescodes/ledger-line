package dev.ledgerline.ledger.application;

import dev.ledgerline.ledger.domain.Account;
import dev.ledgerline.ledger.domain.AccountBalance;

/** An account with its current balance. Holds arrive in Stage 3; until then available = ledger. */
public record AccountView(Account account, AccountBalance balance) {}
