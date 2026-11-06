# ADR-0005: Route every customer and merchant payment through the PSP

- **Status:** Accepted
- **Date:** 2026-11-06
- **Stage:** 0

## Context

The brief fixes several things that a money-flow model must satisfy at once:

- The payment saga is `INITIATED → FUNDS_HELD → PSP_SUBMITTED →
  PSP_CONFIRMED → POSTED → COMPLETED`. Funds are held in Ledgerline's books
  *before* the PSP is asked, so the payer's money is inside the books and the
  PSP sits on the path of a normal payment.
- Stage 10 has a "hot-account" scenario where all payments hit one merchant,
  so merchants have ledger accounts that payments credit.
- The worked examples need wallets (top-up, P2P), merchant payments with fees,
  refunds and chargebacks.
- Stage 5b adds a UPI-style flavour (VPA aliases, collect requests).

In a typical stored-value wallet, a payment between two accounts on the same
platform ("on-us") is a pure ledger transfer, and the PSP is used only when
money enters or leaves. Under that model the brief's saga applies only to
payouts and refunds to cards. The main "create payment" call would never touch
the PSP. The breaker, unknown-outcome and slow-PSP scenarios (Stages 5, 9, 10,
12) would then exercise only side flows.

## Decision

1. **The PSP plays the role of a payment network**, loosely modelled on UPI. It
   authorises and records every payment that a customer or merchant
   initiates, including on-us ones (wallet → wallet, wallet → merchant,
   merchant → wallet refunds). It also moves money between Acme and outside
   bank accounts, and settles the net amount daily, with a settlement file.
2. **Two saga paths:**
   - **Hold path**, when the payer is an Acme account (P2P, wallet-funded
     merchant payment, refund, payout): `INITIATED → FUNDS_HELD →
     PSP_SUBMITTED → PSP_CONFIRMED → POSTED → COMPLETED`, exactly as in the
     brief.
   - **Collect path**, when the payer is outside Acme (top-up, externally
     funded merchant payment): `INITIATED → PSP_SUBMITTED → PSP_CONFIRMED →
     POSTED → COMPLETED`. It skips `FUNDS_HELD` because there is nothing in
     Acme's books to reserve.
3. **Ledger-only postings** (no PSP call) are limited to transactions
   Ledgerline makes itself: the fee leg inside a payment's capture,
   settlement, reconciliation adjustments and reversals.
4. **Chargebacks are not payments.** The PSP reports them through a signed
   webhook, and Acme records a forced debit of the merchant.
5. On-us payments appear in the settlement file as `ON_US` lines with zero net
   cash effect. Reconciliation matches them like any other line.

## Alternatives considered

- **On-us payments are ledger-only; the PSP is used only for pay-ins and
  pay-outs.** This is the more common real-world wallet design, and it is
  simpler and faster for on-us payments. We rejected it because it takes the
  PSP and holds off the main payment path, so most resilience scenarios in
  the brief would test side flows only. Switching later is cheap: the saga
  already supports skipping states, so the change would mostly touch docs
  and tests.
- **Acquiring-only** (every merchant payment is funded from outside, with no
  wallets): this drops the wallet top-up and P2P examples, and the hold path
  appears only in refunds.

## Consequences

- Every customer or merchant payment has an independent external record (the
  PSP reference), so reconciliation covers all payments, not only cash
  movements.
- On-us payments depend on PSP availability. That is mitigated by queueing
  them as `PENDING_PSP` while the breaker is open
  ([ADR-0006](0006-psp-timeouts-are-unknown-outcomes.md)). Their latency
  includes a PSP round trip.
- The scheme rule is a simulation choice. In real wallets, on-us transfers
  usually don't need an external network. DOMAIN.md says so explicitly.
