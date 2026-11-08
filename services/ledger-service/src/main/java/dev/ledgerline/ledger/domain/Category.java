package dev.ledgerline.ledger.domain;

/** Account categories and the side that increases each one (DOMAIN.md §2). */
public enum Category {
    ASSET(Side.DEBIT),
    LIABILITY(Side.CREDIT),
    REVENUE(Side.CREDIT),
    EXPENSE(Side.DEBIT),
    EQUITY(Side.CREDIT);

    private final Side normalSide;

    Category(Side normalSide) {
        this.normalSide = normalSide;
    }

    public Side normalSide() {
        return normalSide;
    }

    /** +1 for debit-normal categories, −1 for credit-normal ones. */
    public int normalSign() {
        return normalSide == Side.DEBIT ? 1 : -1;
    }
}
