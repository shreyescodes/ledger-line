package dev.ledgerline.ledger.domain;

/** The two sides of an entry. Debits count as +amount and credits as −amount (ADR-0003). */
public enum Side {
    DEBIT(1),
    CREDIT(-1);

    private final int sign;

    Side(int sign) {
        this.sign = sign;
    }

    /** The signed (debit-positive) value of an amount on this side. */
    public long signed(long amount) {
        return Math.multiplyExact(amount, (long) sign);
    }

    public Side opposite() {
        return this == DEBIT ? CREDIT : DEBIT;
    }
}
