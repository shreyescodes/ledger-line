# ADR-0006: Treat PSP timeouts as unknown outcomes

- **Status:** Proposed
- **Date:** 2026-11-06
- **Stage:** 0

## Context

When a call to the PSP times out, we don't know whether the PSP executed the
payment. Both obvious reactions lose money:

- **Treat it as failed and compensate** (void the hold). If the PSP did
  execute it, the payee was paid, and the payer gets the money back too: a
  double spend.
- **Retry as a new request.** If the first one succeeded, the payment
  executes twice.

The brief says: "A PSP timeout is NOT a failure: go to PSP_UNKNOWN, poll the
PSP status endpoint with backoff, and hand off to reconciliation if still
unresolved. Never compensate blindly."

## Decision

1. **PSP idempotency.** Every PSP request carries the payment id as the
   PSP-side idempotency key. Retrying the *same* request is safe, because the
   PSP returns the original result instead of executing again. Status checks
   use the same key.
2. **What counts as unknown.** The request may have reached the PSP and we
   have no definitive answer: a timeout, a connection drop after sending, or a
   5xx after the adapter's retries ran out. A connection refused before
   sending is not unknown (the PSP never saw it).
3. **State.** The payment moves to `PSP_UNKNOWN`, and its hold stays ACTIVE.
4. **Resolution sources**, any of which can move it to `PSP_CONFIRMED` or to
   failure:
   - the status endpoint, polled with exponential backoff and jitter within a
     polling budget (default 30 minutes);
   - a verified PSP webhook;
   - reconciliation against the settlement file, after the budget is spent.
     The payment stays `PSP_UNKNOWN`, flagged as escalated.
5. **Definitive negative only.** Compensation (voiding the hold) needs an
   explicit PSP decline or failure, a not-found status after the PSP's dedupe
   window, or reconciliation proof that the PSP never executed it. Absence
   from a settlement file counts only after the `TIMING_DIFF` window
   (default 2 settlement days) closes.
6. **Holds behind submitted or unknown payments never expire.** Expiring them
   would be a blind compensation.
7. **Conflicting outcomes are never applied automatically.** For example, a
   FAILED webhook after a recorded SUCCESS opens an incident.
8. **Circuit breaker open.** New payments are queued as `PENDING_PSP` (funds
   held on the hold path), and the API returns `202 Accepted` with that
   status. They are submitted when the breaker half-opens. A queued payment
   that expires is safe to compensate, because it was never sent.

## Alternatives considered

- **Timeout ⇒ failed**: simple, but it causes double spends (see Context).
- **Timeout ⇒ succeeded**: credits payees for payments that never happened.
- **Retry synchronously until the PSP answers**: ties up request threads,
  multiplies load on a struggling PSP, and still needs a give-up rule.

## Consequences

- In the worst case, a payer's funds stay held for about two days. The
  product must show this as "pending", and support needs a runbook.
- Reconciliation becomes part of the payment lifecycle, not just an
  after-the-fact report.
- Stage 8 needs metrics and alerts on the count and age of `PSP_UNKNOWN` and
  `PENDING_PSP` payments.
