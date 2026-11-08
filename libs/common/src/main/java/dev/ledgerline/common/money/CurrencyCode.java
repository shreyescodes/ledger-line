package dev.ledgerline.common.money;

import java.util.Currency;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * An ISO 4217 currency code such as {@code INR}. The number of decimals comes from ISO 4217 data,
 * never from an assumption (ADR-0002).
 */
public record CurrencyCode(String code) {

    private static final Pattern THREE_LETTERS = Pattern.compile("[A-Z]{3}");

    public CurrencyCode {
        Objects.requireNonNull(code, "code");
        if (!THREE_LETTERS.matcher(code).matches()) {
            throw new IllegalArgumentException("currency code must be three upper-case letters: " + code);
        }
        int digits;
        try {
            digits = Currency.getInstance(code).getDefaultFractionDigits();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown ISO 4217 currency: " + code, e);
        }
        if (digits < 0) {
            throw new IllegalArgumentException("not a currency of account (no minor unit): " + code);
        }
    }

    public static CurrencyCode of(String code) {
        return new CurrencyCode(code);
    }

    /** Decimals of the minor unit: 2 for INR (paise), 0 for JPY, 3 for KWD. */
    public int fractionDigits() {
        return Currency.getInstance(code).getDefaultFractionDigits();
    }

    @Override
    public String toString() {
        return code;
    }
}
