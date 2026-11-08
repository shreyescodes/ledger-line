package dev.ledgerline.common.money;

/**
 * Percentages as integer basis points (1 bp = 0.01%), so fees never touch floating point.
 *
 * <p>A fee is rounded half-up to the minor unit exactly once, and the counterparty gets the rest,
 * so {@code fee + net == amount} always (ADR-0002, invariant I17).
 */
public final class BasisPoints {

    public static final int ONE_HUNDRED_PERCENT = 10_000;

    private BasisPoints() {}

    /** The fee on a non-negative amount at the given rate, rounded half-up to the minor unit. */
    public static Money fee(Money amount, int basisPoints) {
        if (amount.isNegative()) {
            throw new IllegalArgumentException("fees apply to non-negative amounts, got " + amount);
        }
        if (basisPoints < 0 || basisPoints > ONE_HUNDRED_PERCENT) {
            throw new IllegalArgumentException("basis points must be between 0 and 10000, got " + basisPoints);
        }
        long scaled = Math.multiplyExact(amount.amount(), (long) basisPoints);
        long rounded = Math.addExact(scaled, ONE_HUNDRED_PERCENT / 2) / ONE_HUNDRED_PERCENT;
        return Money.of(rounded, amount.currency());
    }

    /** What is left after the fee: {@code amount - fee(amount, basisPoints)}. */
    public static Money net(Money amount, int basisPoints) {
        return amount.minus(fee(amount, basisPoints));
    }
}
