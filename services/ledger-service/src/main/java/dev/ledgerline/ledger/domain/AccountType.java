package dev.ledgerline.ledger.domain;

/** The account types of DOMAIN.md §4.1. The non-negative flag belongs to the type and is absolute. */
public enum AccountType {
    CUSTOMER_WALLET(Category.LIABILITY, true),
    MERCHANT(Category.LIABILITY, false),
    PSP_CLEARING(Category.ASSET, false),
    SETTLEMENT_BANK(Category.ASSET, false),
    FEE_REVENUE(Category.REVENUE, false),
    SUSPENSE(Category.LIABILITY, false),
    WRITE_OFF(Category.EXPENSE, false);

    private final Category category;
    private final boolean nonNegative;

    AccountType(Category category, boolean nonNegative) {
        this.category = category;
        this.nonNegative = nonNegative;
    }

    public Category category() {
        return category;
    }

    public boolean nonNegative() {
        return nonNegative;
    }

    public int normalSign() {
        return category.normalSign();
    }
}
