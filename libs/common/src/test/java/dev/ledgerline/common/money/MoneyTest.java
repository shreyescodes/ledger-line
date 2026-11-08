package dev.ledgerline.common.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void addsAndSubtractsWithinOneCurrency() {
        Money a = Money.of(100_000, "INR");
        Money b = Money.of(25_000, "INR");

        assertThat(a.plus(b)).isEqualTo(Money.of(125_000, "INR"));
        assertThat(a.minus(b)).isEqualTo(Money.of(75_000, "INR"));
        assertThat(b.minus(a).isNegative()).isTrue();
    }

    @Test
    void refusesCrossCurrencyArithmetic() {
        assertThatThrownBy(() -> Money.of(100, "INR").plus(Money.of(100, "USD")))
                .isInstanceOf(CurrencyMismatchException.class)
                .hasMessageContaining("INR")
                .hasMessageContaining("USD");
    }

    @Test
    void overflowFailsInsteadOfWrappingAround() {
        Money max = Money.of(Long.MAX_VALUE, "INR");

        assertThatThrownBy(() -> max.plus(Money.of(1, "INR"))).isInstanceOf(ArithmeticException.class);
        assertThatThrownBy(() -> Money.of(Long.MIN_VALUE, "INR").negate()).isInstanceOf(ArithmeticException.class);
    }

    @Test
    void formatsWithTheCurrencysOwnNumberOfDecimals() {
        assertThat(Money.of(99_999, "INR")).hasToString("INR 999.99");
        assertThat(Money.of(-5, "INR")).hasToString("INR -0.05");
        assertThat(Money.of(1_500, "JPY")).hasToString("JPY 1500");
        assertThat(Money.of(1_500, "KWD")).hasToString("KWD 1.500");
        assertThat(Money.of(Long.MIN_VALUE, "INR")).hasToString("INR -92233720368547758.08");
    }

    @Test
    void currencyCodesMustBeRealIso4217Codes() {
        assertThat(CurrencyCode.of("INR").fractionDigits()).isEqualTo(2);
        assertThat(CurrencyCode.of("JPY").fractionDigits()).isZero();
        assertThatThrownBy(() -> CurrencyCode.of("inr")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CurrencyCode.of("ABC")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CurrencyCode.of("XAU")).isInstanceOf(IllegalArgumentException.class);
    }
}
