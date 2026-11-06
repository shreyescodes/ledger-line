# ADR-0003: Chart of accounts and debit/credit sign convention

- **Status:** Accepted
- **Date:** 2026-11-06
- **Stage:** 0

## Context

The ledger must say which way each account moves, support the zero-sum check
from the brief ("sum of all balances == 0"), and show balances humans can read
("Acme owes Alice ₹1,000.00", not "−100000"). It also needs a fixed set of
account types so that flags like non-negative mean something.

## Decision

1. **Categories and normal sides.** ASSET and EXPENSE are debit-normal.
   LIABILITY, REVENUE and EQUITY are credit-normal. (EQUITY is reserved; no
   account uses it yet.)
2. **Entries carry a direction and a positive amount**: `(account,
   DEBIT|CREDIT, amount > 0, currency)`. This is the domain and API model. How
   the database stores it (a signed `bigint` or a direction column) is decided
   with the schema in Stage 2.
3. **Two views of a balance.**
   - *Raw balance* = Σ(debits) − Σ(credits), debit-positive. Invariants use
     it: per tenant and currency, the raw balances of all accounts sum to 0.
   - *Ledger balance* = the raw balance in the account's normal direction
     (negated for credit-normal accounts). APIs and screens show this one.
4. **Account types** (DOMAIN.md §4.1):

   | Type | Category | Non-negative |
   |---|---|---|
   | CUSTOMER_WALLET | LIABILITY | yes |
   | MERCHANT | LIABILITY | no |
   | PSP_CLEARING | ASSET | no |
   | SETTLEMENT_BANK | ASSET | no |
   | FEE_REVENUE | REVENUE | no |
   | SUSPENSE | LIABILITY | no |
   | WRITE_OFF | EXPENSE | no |

5. **One account per (tenant, holder, type, currency).** Each tenant gets one
   of each system account per currency at onboarding.
6. **Non-negative is a property of the type, and it is absolute.** Wallets are
   non-negative. Merchant accounts are not: chargebacks are forced on Acme by
   the payer's bank, and the ledger must be able to record them even when the
   merchant has no money left. System accounts record facts (cash in the bank,
   money the PSP owes), so they are never flagged.

## Alternatives considered

- **Signed amounts only, no direction or categories**: simpler arithmetic,
  but every reader has to remember which sign means "Acme owes". That is a
  steady source of sign bugs in payment code.
- **Non-negative merchant accounts, with chargeback shortfalls going to a
  receivable account**: keeps every customer-facing account non-negative, but
  needs a "debit up to the available balance, put the rest elsewhere"
  primitive and an extra account per merchant. Worth revisiting if we add
  rolling reserves against chargebacks.
- **Per-posting overdraft permission** (instead of per-account flags):
  flexible, but it turns invariant 5 into a convention that every caller must
  respect, rather than something the ledger enforces.

## Consequences

- APIs and docs use debit/credit vocabulary, which DOMAIN.md §2 explains.
- A negative MERCHANT balance is legitimate. It blocks refunds and payouts
  (there is no available balance to hold) until new sales cover it.
- The invariant verifier (Stage 2) checks raw balances sum to zero per tenant
  and currency.
