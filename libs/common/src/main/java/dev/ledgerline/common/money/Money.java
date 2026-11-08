package dev.ledgerline.common.money;

import java.util.Objects;

/**
 * An amount of money: a whole number of minor units (paise for INR) in one currency.
 *
 * <p>No floating point anywhere, arithmetic only within one currency, and overflow throws instead
 * of wrapping around (invariant I1, ADR-0002).
 */
public record Money(long amount, CurrencyCode currency) {

    public Money {
        Objects.requireNonNull(currency, "currency");
    }

    public static Money of(long minorUnits, CurrencyCode currency) {
        return new Money(minorUnits, currency);
    }

    public static Money of(long minorUnits, String currency) {
        return new Money(minorUnits, CurrencyCode.of(currency));
    }

    public static Money zero(CurrencyCode currency) {
        return new Money(0, currency);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.addExact(amount, other.amount), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.subtractExact(amount, other.amount), currency);
    }

    public Money negate() {
        return new Money(Math.negateExact(amount), currency);
    }

    public boolean isPositive() {
        return amount > 0;
    }

    public boolean isNegative() {
        return amount < 0;
    }

    public boolean isZero() {
        return amount == 0;
    }

    public boolean isAtLeast(Money other) {
        requireSameCurrency(other);
        return amount >= other.amount;
    }

    /** For people, not for code: "INR 1234.56". */
    @Override
    public String toString() {
        int digits = currency.fractionDigits();
        if (digits == 0) {
            return currency + " " + amount;
        }
        String sign = amount < 0 ? "-" : "";
        // Work on the magnitude as a string so Long.MIN_VALUE can't overflow on negation.
        String magnitude = Long.toString(amount).replace("-", "");
        if (magnitude.length() <= digits) {
            magnitude = "0".repeat(digits - magnitude.length() + 1) + magnitude;
        }
        int point = magnitude.length() - digits;
        return currency + " " + sign + magnitude.substring(0, point) + "." + magnitude.substring(point);
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new CurrencyMismatchException(currency, other.currency);
        }
    }
}
