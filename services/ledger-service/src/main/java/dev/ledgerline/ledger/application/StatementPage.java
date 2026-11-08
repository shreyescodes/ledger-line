package dev.ledgerline.ledger.application;

import java.util.List;
import java.util.OptionalLong;

/**
 * A page of a statement. {@code nextCursor} is the account sequence number to pass as
 * {@code after} for the next page; empty on the last page.
 */
public record StatementPage(List<StatementLine> lines, OptionalLong nextCursor) {

    public StatementPage {
        lines = List.copyOf(lines);
    }
}
