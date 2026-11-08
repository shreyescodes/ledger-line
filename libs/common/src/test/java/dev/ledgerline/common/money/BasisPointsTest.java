package dev.ledgerline.common.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.ledgerline.testsupport.property.PropertyCheck;
import org.junit.jupiter.api.Test;

class BasisPointsTest {

    private record Case(long amount, int basisPoints) {}

    @Test
    void theWorkedExampleFromDomainMd() {
        // 2.00% of ₹999.99 is 1999.98 paise, rounded half-up to 2000; the merchant gets 97999.
        Money amount = Money.of(99_999, "INR");

        assertThat(BasisPoints.fee(amount, 200)).isEqualTo(Money.of(2_000, "INR"));
        assertThat(BasisPoints.net(amount, 200)).isEqualTo(Money.of(97_999, "INR"));
    }

    @Test
    void roundsHalfUpExactlyOnce() {
        assertThat(BasisPoints.fee(Money.of(25, "INR"), 200)).isEqualTo(Money.of(1, "INR")); // 0.5 -> 1
        assertThat(BasisPoints.fee(Money.of(24, "INR"), 200)).isEqualTo(Money.of(0, "INR")); // 0.48 -> 0
    }

    @Test
    void feePlusNetIsAlwaysTheWholeAmount() {
        PropertyCheck.forAll(random -> new Case(random.nextLong(0, 1_000_000_000_000L), random.nextInt(0, 10_001)))
                .tries(10_000)
                .check(c -> {
                    Money amount = Money.of(c.amount(), "INR");
                    Money fee = BasisPoints.fee(amount, c.basisPoints());
                    assertThat(fee.plus(BasisPoints.net(amount, c.basisPoints())))
                            .isEqualTo(amount);
                    assertThat(fee.amount()).isBetween(0L, c.amount());
                });
    }

    @Test
    void refusesNegativeAmountsAndImpossibleRates() {
        assertThatThrownBy(() -> BasisPoints.fee(Money.of(-1, "INR"), 200))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BasisPoints.fee(Money.of(1, "INR"), 10_001))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
