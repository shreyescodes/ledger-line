# ADR-0002: Represent money as integer minor units

- **Status:** Proposed
- **Date:** 2026-11-06
- **Stage:** 0

## Context

Invariant 1 of the brief: money is an integer count of minor units plus an ISO
4217 currency code, with no floating point and no arithmetic across currencies.
This ADR pins down the details the invariant leaves open: how big a number can
be, how percentages round, and how amounts cross the API boundary to a
JavaScript frontend.

Forces:

- `0.1 + 0.2 != 0.3` in binary floating point, so `double` is out.
- `BigDecimal` is exact, but its scale makes `1.0` and `1.00` unequal under
  `equals`. It also allows sub-minor amounts (₹0.001) that no bank can settle,
  and it is slower and heavier in hot paths.
- JavaScript numbers are IEEE doubles. Integers are exact only up to
  2^53 − 1 = 9,007,199,254,740,991.
- Currencies have different numbers of decimals: INR 2, JPY 0, KWD 3, CLF 4.

## Decision

1. **Type.** `Money(long amount, CurrencyCode currency)`, where `amount` counts
   minor units (paise for INR). It is immutable and a value object.
2. **Decimals come from ISO 4217 data** (in Java: `java.util.Currency`'s
   default fraction digits). Never assume 2.
3. **Same currency or nothing.** `plus`, `minus` and comparisons throw if the
   currencies differ. Each account has exactly one currency. FX is a non-goal;
   if it is ever added, it is two single-currency legs through an FX account.
4. **Overflow is an error.** Arithmetic uses `Math.addExact` and friends. A
   wrapped `long` is a bug that must fail loudly.
5. **Rates are integer basis points** (1 bp = 0.01%). A fee is
   `round_half_up(amount × bps / 10 000)`, computed once in integer arithmetic.
   The counterparty receives `amount − fee`, so `fee + net == amount`
   exactly. The rounding mode lives in a single fee calculator, so changing it
   is a one-line change plus tests.
6. **Storage.** Postgres `bigint` for amounts and `char(3)` for currency
   codes. Never `numeric` with a fractional scale, and never `real`/`double`.
7. **API.** Amounts are JSON integers in minor units:
   `{"amount": 100000, "currency": "INR"}` is ₹1,000.00. Each request amount
   is validated to `1 ≤ amount ≤ 10^12` (₹10 billion for INR), which stays far
   below 2^53, so the TypeScript dashboard reads every value exactly.

## Alternatives considered

- **`BigDecimal` everywhere**: exact, but the scale pitfalls are real and it
  allows amounts that can't be settled. We may still use it inside a
  calculation (e.g. a future FX rate) as long as the result is converted back
  to minor units with an explicit rounding mode.
- **Decimal strings in JSON** (`"1000.00"`): safe in every language, but
  clunky, and both ends must format and parse decimals. Revisit if amounts
  near 2^53 ever become realistic.
- **Round half-even (banker's rounding)**: less bias across very many
  rounding events. We chose half-up because it matches how fee schedules are
  usually stated and checked by hand, and rounding happens only once per fee.

## Consequences

- Formatting for humans needs the currency's decimal count. A helper handles
  it; display formatting never happens in the domain layer.
- An ArchUnit rule (Stage 1/2) will ban `float`/`double` in domain packages.
- jqwik property tests (Stage 2) will check `fee + net == amount` and the
  currency-mismatch failures.
